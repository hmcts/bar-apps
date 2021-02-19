package uk.gov.hmcts.bar.api.data.service;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import uk.gov.hmcts.bar.api.componenttests.ComponentTestBase;
import uk.gov.hmcts.bar.api.data.model.BarUser;
import uk.gov.hmcts.bar.api.data.model.UserDetails;
import uk.gov.hmcts.bar.api.data.repository.BarUserRepository;
import uk.gov.hmcts.bar.api.security.utils.SecurityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BarUserServiceTest {

    @InjectMocks
    private BarUserService barUserService;

    @Mock
    private BarUserRepository barUserRepository;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private SecurityUtils securityUtils;

    private SecurityContext securityContext;
    private BarUser barUser;

    private String siteApi = "http://localhost:23445";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(cacheManager.getCache(Mockito.anyString())).thenReturn(new ConcurrentMapCache("barusers"));
        barUserService = new BarUserService(barUserRepository, httpClient, siteApi, cacheManager, securityUtils);


        Authentication authentication = new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return null;
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return new UserDetails("username", "token", Arrays.asList("bar-super-user"));
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

            }

            @Override
            public String getName() {
                return null;
            }
        };

        securityContext = new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return authentication;
            }

            @Override
            public void setAuthentication(Authentication authentication) {

            }
        };

        barUser = new BarUser("user1", Collections.emptySet(), "user1@mail.com", "user", "one");
    }

    @Test(expected = AccessDeniedException.class)
    public void whenSecurityContextIsInvalid_shouldReceiveAccessDeniedException() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        when(securityUtils.getUserInfo()).thenReturn(ComponentTestBase.getUserInfoBasedOnUID_Roles(null,"invalid-role"));
        barUserService.getCurrentUserId();
    }

    @Test
    public void whenSecurityContextIsValid_shouldGetUserId(){
        SecurityContextHolder.setContext(securityContext);
        when(securityUtils.getUserInfo()).thenReturn(ComponentTestBase.getUserInfoBasedOnUID_Roles("Valid-UID","bar-post-clerk"));

        assertEquals("Valid-UID", barUserService.getCurrentUserId());
    }

    @Test
    public void whenUserAlreadyInTheDb_shouldNotCallSave() {
        when(barUserRepository.findBarUserById(Mockito.anyString())).thenReturn(barUser);
        barUserService.saveUser(barUser);
        verify(barUserRepository, times(1)).findBarUserById(anyString());
        verify(barUserRepository, times(0)).save(any(BarUser.class));
    }

    @Test
    public void whenUserIsNotInTheDb_shoulCallSave() {
        when(barUserRepository.findBarUserById(Mockito.anyString())).thenReturn(null);
        when(barUserRepository.save(barUser)).thenReturn(barUser);
        barUserService.saveUser(barUser);
        verify(barUserRepository, times(1)).findBarUserById(anyString());
        verify(barUserRepository, times(1)).save(any(BarUser.class));
    }

    @Test
    public void whenUserIsInTheDb_butModified_thenSaveIsCalledToUpdate() {
        BarUser modifiedUser = new BarUser("user1", Collections.emptySet(), "user1@mail.com", "user", "one plus one");
        when(barUserRepository.findBarUserById(Mockito.anyString())).thenReturn(barUser);
        when(barUserRepository.save(modifiedUser)).thenReturn(modifiedUser);
        barUserService.saveUser(modifiedUser);
        verify(barUserRepository, times(1)).findBarUserById(anyString());
        verify(barUserRepository, times(1)).save(any(BarUser.class));
    }

    @Test
    public void testValidateUserAgainstSiteWhenValid() throws IOException {
        String email = "user@gmail.com";
        String token = "this_is_a_user_token";
        String siteId = "siteId";
        when(httpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> new SiteHttpResponse(200, "true"));
        assertTrue(barUserService.validateUserAgainstSite(email, token, siteId));
    }

    @Test
    public void testValidateUserAgainstSiteWhenInValid() throws IOException {
        String email = "user@gmail.com";
        String token = "this_is_a_user_token";
        String siteId = "siteId";
        when(httpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> new SiteHttpResponse(200, "false"));
        assertFalse(barUserService.validateUserAgainstSite(email, token, siteId));
    }

    @Test
    public void testValidateUserAgainstSiteWhenAnyParameterIsBlank() throws IOException {
        String email = "user@gmail.com";
        String token = "this_is_a_user_token";
        String siteId = "siteId";
        assertFalse(barUserService.validateUserAgainstSite("", token, siteId));
        assertFalse(barUserService.validateUserAgainstSite(null, token, siteId));
        assertFalse(barUserService.validateUserAgainstSite(" ", token, siteId));

        assertFalse(barUserService.validateUserAgainstSite(email, " ", siteId));
        assertFalse(barUserService.validateUserAgainstSite(email, "", siteId));
        assertFalse(barUserService.validateUserAgainstSite(email, null, siteId));

        assertFalse(barUserService.validateUserAgainstSite(email, token, ""));
        assertFalse(barUserService.validateUserAgainstSite(email, token, " "));
        assertFalse(barUserService.validateUserAgainstSite(email, token, null));
    }

    @Test
    public void checkSiteVaidationRequest() throws IOException {
        String email = "user@gmail.com";
        String token = "this_is_a_user_token";
        String siteId = "siteId";
        when(httpClient.execute(any(HttpGet.class))).thenAnswer(invocation -> {
            HttpGet httpGet = invocation.getArgument(0);
            assertThat(httpGet.getMethod(), Is.is("GET"));
            assertThat(httpGet.getURI().toString(), Is.is("http://localhost:23445/sites/siteId/users/user@gmail.com"));
            assertThat(httpGet.getHeaders("Authorization")[0].getValue(), Is.is("this_is_a_user_token"));
            return new SiteHttpResponse(200, "true");
        });
        barUserService.validateUserAgainstSite(email, token, siteId);
    }

    public static class SiteHttpResponse implements CloseableHttpResponse {

        private String message;
        private int responseCode;

        SiteHttpResponse(int responseCode, String message){
            this.responseCode = responseCode;
            this.message = message;
        }

        @Override
        public void close() throws IOException {
            // Not implemented
        }

        @Override
        public StatusLine getStatusLine() {
            return new BasicStatusLine(
                new ProtocolVersion("http", 1, 1),
                this.responseCode,
                "OK"
            );
        }

        @Override
        public void setStatusLine(StatusLine statusLine) {
            // Not implemented
        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i) {
            // Not implemented
        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i, String s) {
            // Not implemented
        }

        @Override
        public void setStatusCode(int i) throws IllegalStateException {
            // Not implemented
        }

        @Override
        public void setReasonPhrase(String s) throws IllegalStateException {
            // Not implemented
        }

        @Override
        public HttpEntity getEntity() {
            try {
                return new StringEntity(this.message);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void setEntity(HttpEntity httpEntity) {
            // Not implemented
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public void setLocale(Locale locale) {
            // Not implemented
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            return null;
        }

        @Override
        public boolean containsHeader(String s) {
            return false;
        }

        @Override
        public Header[] getHeaders(String s) {
            return new Header[0];
        }

        @Override
        public Header getFirstHeader(String s) {
            return null;
        }

        @Override
        public Header getLastHeader(String s) {
            return null;
        }

        @Override
        public Header[] getAllHeaders() {
            return new Header[0];
        }

        @Override
        public void addHeader(Header header) {
            // Not implemented
        }

        @Override
        public void addHeader(String s, String s1) {
            // Not implemented
        }

        @Override
        public void setHeader(Header header) {
            // Not implemented
        }

        @Override
        public void setHeader(String s, String s1) {
            // Not implemented
        }

        @Override
        public void setHeaders(Header[] headers) {
            // Not implemented
        }

        @Override
        public void removeHeader(Header header) {
            // Not implemented
        }

        @Override
        public void removeHeaders(String s) {
            // Not implemented
        }

        @Override
        public HeaderIterator headerIterator() {
            return null;
        }

        @Override
        public HeaderIterator headerIterator(String s) {
            return null;
        }

        @Override
        public HttpParams getParams() {
            return null;
        }

        @Override
        public void setParams(HttpParams httpParams) {
            // Not implemented
        }
    }
}
