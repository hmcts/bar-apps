package uk.gov.hmcts.bar.api.security.exception;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class BarAccessDeniedHandlerTest {

    @Autowired
    BarAccessDeniedHandler barAccessDeniedHandler;

    MockHttpServletRequest request;

    MockHttpServletResponse response;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        barAccessDeniedHandler = new BarAccessDeniedHandler();
    }

    @Test
    public void testHandle() throws IOException, ServletException {
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access is denied");
        barAccessDeniedHandler.handle(request, response, accessDeniedException);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }
}
