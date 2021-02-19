package uk.gov.hmcts.bar.api.security.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class BarAuthenticationEntryPointTest {

    @Autowired
    BarAuthenticationEntryPoint barAuthenticationEntryPoint;

    MockHttpServletRequest request;

    MockHttpServletResponse response;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        barAuthenticationEntryPoint = new BarAuthenticationEntryPoint();
    }

    @Test
    public void testCommence() throws IOException, ServletException {
        AuthenticationException authenticationException = new InvalidBearerTokenException("Bearer token is invalid");
        barAuthenticationEntryPoint.commence(request, response, authenticationException);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }
}
