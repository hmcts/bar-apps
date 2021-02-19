package uk.gov.hmcts.bar.api.security.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.bar.api.data.model.BarUser;
import uk.gov.hmcts.bar.api.data.service.BarUserService;
import uk.gov.hmcts.bar.api.security.exception.UnauthorizedException;
import uk.gov.hmcts.bar.api.security.utils.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Custom filter responsible for User authorisation
 */

public class UserAuthFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(UserAuthFilter.class);

    private final Function<HttpServletRequest, Optional<String>> userIdExtractor;
    private final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor;
    private final SecurityUtils securityUtils;
    private final BarUserService barUserService;

    public UserAuthFilter(Function<HttpServletRequest, Optional<String>> userIdExtractor,
                          Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                          SecurityUtils securityUtils, BarUserService barUserService) {
        super();
        this.userIdExtractor = userIdExtractor;
        this.authorizedRolesExtractor = authorizedRolesExtractor;
        this.securityUtils = securityUtils;
        this.barUserService = barUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Collection<String> authorizedRoles = authorizedRolesExtractor.apply(request);
        Optional<String> userIdOptional = userIdExtractor.apply(request);

        if (SecurityUtils.isAuthenticated() && (!authorizedRoles.isEmpty() || userIdOptional.isPresent())) {
            try {
                verifyRoleAndUserId(authorizedRoles, userIdOptional);
                LOG.info("User authentication is successful");
            } catch (UnauthorizedException ex) {
                LOG.warn("Unauthorised roles or userId in the request path", ex);
                response.sendError(FORBIDDEN.value(), " Access Denied " + ex.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void verifyRoleAndUserId(Collection<String> authorizedRoles, Optional<String> userIdOptional) {
        UserInfo userInfo = securityUtils.getUserInfo();
        if (!authorizedRoles.isEmpty() && Collections.disjoint(authorizedRoles, userInfo.getRoles())) {

            Optional<List<String>> currentRolesOptional = Optional.ofNullable(userInfo.getRoles());
            List<String> currentRoles = null;
            if (currentRolesOptional.isPresent() && !currentRolesOptional.get().isEmpty()){
                currentRoles = currentRolesOptional.get();
            }
            throw new UnauthorizedException("Current user roles are : " + currentRoles +
                                                " While Authorised roles are only : " +authorizedRoles);

        }
        userIdOptional.ifPresent(resourceUserId -> {
            if (!resourceUserId.equalsIgnoreCase(userInfo.getUid())) {
                throw new UnauthorizedException("Unauthorised userId in the path");
            }
        });
        //save Bar User
        saveBarUser(userInfo);
    }

    private void saveBarUser(UserInfo userInfo) {
        BarUser barUser = new BarUser(
            userInfo.getUid(),
            userInfo.getRoles().stream().collect(Collectors.toSet()),
            userInfo.getSub(),
            userInfo.getGivenName(),
            userInfo.getFamilyName()
        );
        barUserService.saveUser(barUser);
    }

}
