package uk.gov.hmcts.bar.api.componenttests.sugar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

public class RestActions {
    public static final MediaType TEXT_CSV = new MediaType("text", "csv");

    public static final String DEFAULT_SITE_ID = "Y431";

    public static String SITEID_HEADER = "SiteId";

    private final HttpHeaders httpHeaders = new HttpHeaders();
    private final MockMvc mvc;
    private final ObjectMapper objectMapper;
    private final UserDetails userDetails;
    private final JwtAuthenticationToken jwtAuthenticationToken;
    private final UserInfo userInfo;

    public RestActions(MockMvc mvc, ObjectMapper objectMapper, UserDetails userDetails, JwtAuthenticationToken jwtAuthenticationToken, UserInfo userInfo) {
        this.mvc = mvc;
        this.objectMapper = objectMapper;
        this.userDetails = userDetails;
        this.jwtAuthenticationToken = jwtAuthenticationToken;
        this.userInfo = userInfo;
        this.httpHeaders.add("Authorization", "DummyBearerToken");
    }

    public ResultActions get(String urlTemplate, String siteId) {
        setSecurityContext();
        addSiteIdHeader(siteId);
        try {
            return mvc.perform(MockMvcRequestBuilders
                .get(urlTemplate)
                .with(authentication(jwtAuthenticationToken))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .headers(httpHeaders));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultActions get(String urlTemplate) {
        return get(urlTemplate, DEFAULT_SITE_ID);
    }

    public ResultActions getCsv(String urlTemplate,String siteId) {
        setSecurityContext();
        List mediatypes = new ArrayList();
        mediatypes.add(new MediaType("text", "csv"));
        httpHeaders.setAccept(mediatypes);
        httpHeaders.set(SITEID_HEADER, siteId);
        try {
            return mvc.perform(MockMvcRequestBuilders
                .get(urlTemplate)
                .with(authentication(jwtAuthenticationToken))
                .contentType(APPLICATION_JSON)
                .accept(TEXT_CSV)
                .headers(httpHeaders));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultActions put(String urlTemplate, Object dto, String siteId) {
        setSecurityContext();
        addSiteIdHeader(siteId);
        try {
            return mvc.perform(MockMvcRequestBuilders
                .put(urlTemplate)
                .with(authentication(jwtAuthenticationToken))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .headers(httpHeaders)
                .content(objectMapper.writeValueAsString(dto))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultActions put(String urlTemplate, Object dto) {
        return put(urlTemplate, dto, DEFAULT_SITE_ID);
    }

    public ResultActions post(String urlTemplate, Object dto, String siteId) {
        setSecurityContext();
        addSiteIdHeader(siteId);
        try {
            return mvc.perform(MockMvcRequestBuilders
                .post(urlTemplate)
                .with(authentication(jwtAuthenticationToken))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .headers(httpHeaders)
                .content(objectMapper.writeValueAsString(dto))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultActions post(String urlTemplate, Object dto) {
        return post(urlTemplate, dto, DEFAULT_SITE_ID);
    }

    public ResultActions delete(String urlTemplate, Object... uriVars) {
        return delete(urlTemplate, DEFAULT_SITE_ID, uriVars);
    }

    public ResultActions delete(String urlTemplate, String siteId, Object... uriVars) {
        setSecurityContext();
        addSiteIdHeader(siteId);
        try {
            return mvc.perform(MockMvcRequestBuilders
                .delete(urlTemplate, uriVars)
                .with(authentication(jwtAuthenticationToken))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .headers(httpHeaders)
            	    .content(objectMapper.writeValueAsString(uriVars)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultActions patch(String urlTemplate, Object dto) {
        return patch(urlTemplate, dto, DEFAULT_SITE_ID);
    }

    public ResultActions patch(String urlTemplate, Object dto, String siteId) {
        setSecurityContext();
        addSiteIdHeader(siteId);
        try {
            return mvc.perform(MockMvcRequestBuilders
                .patch(urlTemplate, dto)
                .with(authentication(jwtAuthenticationToken))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .headers(httpHeaders)
                .content(objectMapper.writeValueAsString(dto)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addSiteIdHeader(String siteId) {
        httpHeaders.set(SITEID_HEADER, siteId);
    }

    private void setSecurityContext() {
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails,null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public UserInfo getUserInfoForRestAction() {
        return this.userInfo;
    }
}

