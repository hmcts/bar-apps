package uk.gov.hmcts.bar.api.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.GenericFilterBean;
import uk.gov.hmcts.bar.api.data.model.BarUser;
import uk.gov.hmcts.bar.api.data.service.BarUserService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SiteValidationFilter extends GenericFilterBean {

    private final BarUserService barUserService;

    private static final String[] excludeUrlPatterns = {"/swagger-ui.html", "/webjars/springfox-swagger-ui/**", "/swagger-resources/**",
        "/v2/**", "/health","/health/liveness", "/health/readiness", "/payment-types", "/info", "/sites/**"};

    private static AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    public SiteValidationFilter(BarUserService barUserService) {
        this.barUserService = barUserService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        boolean shouldExclude = Arrays.stream(excludeUrlPatterns).anyMatch(p -> pathMatcher.match(p, httpServletRequest.getServletPath()));
        if (shouldExclude) {
            chain.doFilter(request, response);
            return;
        }
        try {
            BarUser barUser = barUserService.getBarUser().orElseThrow(() -> new UserValidationException("Failed to retrieve authenticated user"));
            String authHeader = httpServletRequest.getHeader("Authorization");
            String siteId = httpServletRequest.getHeader("SiteId");
            if (barUserService.validateUserAgainstSite(barUser.getEmail(), authHeader, siteId)) {
                barUser.setSelectedSiteId(siteId);
                chain.doFilter(new BarWrappedHttpRequest(((HttpServletRequest) request), barUser), response);
            } else {
                throw new UserValidationException("Failed to validate user against the given site");
            }
        } catch (UserValidationException e) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }
}
