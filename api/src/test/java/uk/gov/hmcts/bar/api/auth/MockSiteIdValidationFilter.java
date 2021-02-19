package uk.gov.hmcts.bar.api.auth;

import uk.gov.hmcts.bar.api.data.model.BarUser;
import uk.gov.hmcts.bar.api.data.model.UserDetails;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

public class MockSiteIdValidationFilter implements Filter {

    private BarUser barUser;

    public MockSiteIdValidationFilter(UserDetails barUserDetails) {
        this.barUser = BarUser.builder().id(barUserDetails.getUsername())
            .surname(barUserDetails.getUsername() + "-fn")
            .forename(barUserDetails.getUsername() + "-ln")
            .roles(barUserDetails.getAuthorities().stream().map(Object::toString).collect(Collectors.toSet()))
            .email(barUserDetails.getUsername() + "@hmcts.net").build();
        barUser.setSelectedSiteId("Y431");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new BarWrappedHttpRequest((HttpServletRequest) request, barUser), response);
    }
}
