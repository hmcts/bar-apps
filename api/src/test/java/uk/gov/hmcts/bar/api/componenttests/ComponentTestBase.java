package uk.gov.hmcts.bar.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.bar.api.BarServiceApplication;
import uk.gov.hmcts.bar.api.auth.SiteValidationFilter;
import uk.gov.hmcts.bar.api.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.bar.api.componenttests.sugar.RestActions;
import uk.gov.hmcts.bar.api.componenttests.utils.DbTestUtil;
import uk.gov.hmcts.bar.api.data.model.UserDetails;
import uk.gov.hmcts.bar.api.data.service.BarUserService;
import uk.gov.hmcts.bar.api.security.idam.IdamRepository;
import uk.gov.hmcts.bar.multisite.MultisiteConfiguration;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;


import javax.ws.rs.core.MediaType;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {BarServiceApplication.class, MultisiteConfiguration.class}, webEnvironment = MOCK)
@ActiveProfiles({"embedded", "idam-backdoor"})
public class ComponentTestBase {

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule( options().port(23445).notifier(new ConsoleNotifier(true)));

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BarUserService barUserService;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private IdamRepository idamRepository;

    public static final String BAR_POST_CLERK_AUTHORITY = "bar-post-clerk";
    public static final String BAR_FEE_CLERK_AUTHORITY = "bar-fee-clerk";
    public static final String BAR_SR_FEE_CLERK_AUTHORITY = "bar-senior-clerk";
    public static final String BAR_DM_AUTHORITY = "bar-delivery-manager";

    public final UserDetails userDetails =
        new UserDetails("test123@hmcts.net", "abc123", Collections.singletonList("bar-post-clerk"));
    public final UserDetails feeClerkUserDetails =
            new UserDetails("fee-clerk", "abc123", Collections.singletonList("bar-fee-clerk"));
    public final UserDetails srFeeClerkUserDetails =
            new UserDetails("sr-fee-clerk", "abc123", Collections.singletonList("bar-senior-clerk"));
    public final UserDetails dmUserDetails =
            new UserDetails("dm-manager", "abc123", Collections.singletonList("bar-delivery-manager"));
    public final UserDetails adminUserDetails =
        new UserDetails("admin-site2", "abc123", Collections.singletonList("super"));
    public final UserDetails postClerkUserDetailsSite2 =
        new UserDetails("post-clerk-site2", "abc123", Collections.singletonList("bar-post-clerk"));
    public final UserDetails feeClerkUserDetailsSite2 =
        new UserDetails("fee-clerk-site2", "abc123", Collections.singletonList("bar-fee-clerk"));
    public final UserDetails srFeeClerkUserDetailsSite2 =
        new UserDetails("sr-fee-clerk-site2", "abc123", Collections.singletonList("bar-senior-clerk"));
    public final UserDetails dmUserUserDetailsSite2 =
        new UserDetails("dm-manager-site2", "abc123", Collections.singletonList("bar-delivery-manager"));
    public final UserDetails adminUserDetailsSite2 =
        new UserDetails("admin-site2", "abc123", Collections.singletonList("super"));


    public RestActions restActions;
    public RestActions restActionsForFeeClerk;
    public RestActions restActionsForSrFeeClerk;
    public RestActions restActionsForDM;
    public RestActions restActionsForAdmin;
    public RestActions restActionsPostClerkSite2;
    public RestActions restActionsForFeeClerkSite2;
    public RestActions restActionsForSrFeeClerkSite2;
    public RestActions restActionsForDMSite2;
    public RestActions restActionsForAdminSite2;

    @Before
    public void setUp() throws Exception {
        DefaultMockMvcBuilder mvc = webAppContextSetup(webApplicationContext).apply(springSecurity());
        UserInfo userInfo;
        when(idamRepository.getUserInfo(contains(BAR_POST_CLERK_AUTHORITY))).thenReturn(userInfo=getUserInfoBasedOnUID_Roles(UUID.randomUUID().toString(), BAR_POST_CLERK_AUTHORITY));
        this.restActions = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, userDetails, getJWTAuthenticationTokenBasedOnRoles(BAR_POST_CLERK_AUTHORITY), userInfo);

        when(idamRepository.getUserInfo(contains(BAR_FEE_CLERK_AUTHORITY))).thenReturn(userInfo=getUserInfoBasedOnUID_Roles(UUID.randomUUID().toString(), BAR_FEE_CLERK_AUTHORITY));
        this.restActionsForFeeClerk = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, feeClerkUserDetails, getJWTAuthenticationTokenBasedOnRoles(BAR_FEE_CLERK_AUTHORITY), userInfo);

        when(idamRepository.getUserInfo(contains(BAR_SR_FEE_CLERK_AUTHORITY))).thenReturn(userInfo=getUserInfoBasedOnUID_Roles(UUID.randomUUID().toString(), BAR_SR_FEE_CLERK_AUTHORITY));
        this.restActionsForSrFeeClerk = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, srFeeClerkUserDetails, getJWTAuthenticationTokenBasedOnRoles(BAR_SR_FEE_CLERK_AUTHORITY), userInfo);

        when(idamRepository.getUserInfo(contains(BAR_DM_AUTHORITY))).thenReturn(userInfo=getUserInfoBasedOnUID_Roles(UUID.randomUUID().toString(), BAR_DM_AUTHORITY));
        this.restActionsForDM = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, dmUserDetails, getJWTAuthenticationTokenBasedOnRoles(BAR_DM_AUTHORITY), userInfo);

        this.restActionsForAdmin = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, adminUserDetails, null, null);
        this.restActionsPostClerkSite2 = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, postClerkUserDetailsSite2, null, null);
        this.restActionsForFeeClerkSite2 = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, feeClerkUserDetailsSite2, null, null);
        this.restActionsForSrFeeClerkSite2 = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, srFeeClerkUserDetailsSite2, null, null);
        this.restActionsForDMSite2 = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, dmUserUserDetailsSite2, null, null);
        this.restActionsForAdminSite2 = new RestActions(mvc.addFilter(new SiteValidationFilter(barUserService)).build(), objectMapper, adminUserDetailsSite2, null, null);

        DbTestUtil.emptyTable(webApplicationContext, "payment_instruction_status");
        DbTestUtil.emptyTable(webApplicationContext, "case_fee_detail");
        DbTestUtil.emptyTable(webApplicationContext, "payment_instruction_status");
        DbTestUtil.emptyTable(webApplicationContext, "bar_user");
        DbTestUtil.emptyTable(webApplicationContext, "payment_instruction_payhub_reference");
        DbTestUtil.emptyTable(webApplicationContext, "payment_instruction_status");
        DbTestUtil.emptyTable(webApplicationContext, "payment_instruction");
        DbTestUtil.emptyTable(webApplicationContext, "user_site");
        DbTestUtil.emptyTable(webApplicationContext, "site");
        DbTestUtil.resetAutoIncrementColumns(webApplicationContext, "payment_instruction");
        DbTestUtil.addTestUser(webApplicationContext, userDetails);
        DbTestUtil.addTestUser(webApplicationContext, feeClerkUserDetails);
        DbTestUtil.addTestUser(webApplicationContext, srFeeClerkUserDetails);
        DbTestUtil.addTestUser(webApplicationContext, dmUserDetails);
        DbTestUtil.addTestUser(webApplicationContext, adminUserDetails);
        DbTestUtil.addTestSiteUser(webApplicationContext);

        wireMockRule.stubFor(get(urlPathMatching("/sites/(.+)/users/(.+)"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.TEXT_PLAIN)
                .withBody("true")
            )
        );
    }


    public CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    public WebApplicationContext getWebApplicationContext() {
        return webApplicationContext;
    }

    public ObjectMapper getObjectMapper(){
        return objectMapper;
    }

    public static UserInfo getUserInfoBasedOnUID_Roles(String UID, String roles) {
        String identifier = roles + getRandomNumber(0, 1000);
        return UserInfo.builder()
            .uid(UID)
            .sub(identifier + "@hmcts.net")
            .givenName(identifier + " GivenName")
            .familyName(identifier + " FamilyName")
            .roles(Arrays.asList(roles))
            .build();
    }

    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static JwtAuthenticationToken getJWTAuthenticationTokenBasedOnRoles(String authority) {
        List<String> stringGrantedAuthority = new ArrayList();
        stringGrantedAuthority.add(authority);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("roles", stringGrantedAuthority);

        Map<String, Object> headersMap = new HashMap<>();
        headersMap.put("authorisation", "test-token");

        Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(authority);
        Jwt jwt = new Jwt("test_token_" + stringGrantedAuthority, null, null, headersMap, claimsMap);
        return new JwtAuthenticationToken(jwt, authorities);
    }
}

