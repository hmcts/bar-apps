package uk.gov.hmcts.bar.api.configuration;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.bar.api.data.service.BarUserService;
import uk.gov.hmcts.bar.api.security.converter.BarJwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.bar.api.security.exception.BarAccessDeniedHandler;
import uk.gov.hmcts.bar.api.security.exception.BarAuthenticationEntryPoint;
import uk.gov.hmcts.bar.api.security.filters.UserAuthFilter;
import uk.gov.hmcts.bar.api.security.utils.SecurityUtils;
import uk.gov.hmcts.bar.api.security.validator.AudienceValidator;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity(debug = true)
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityConfiguration.class);

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.audience-list}")
    private String[] allowedAudiences;

    @Value("${oidc.issuer}")
    private String issuerOverride;

    private final UserAuthFilter userAuthFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final BarAuthenticationEntryPoint barAuthenticationEntryPoint;
    private final BarAccessDeniedHandler barAccessDeniedHandler;

    @Autowired
    public SpringSecurityConfiguration(final BarJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                       final Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                       final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                                       final SecurityUtils securityUtils, final BarAuthenticationEntryPoint barAuthenticationEntryPoint,
                                       final BarAccessDeniedHandler barAccessDeniedHandler, final BarUserService barUserService) {
        super();
        this.userAuthFilter = new UserAuthFilter(
            userIdExtractor, authorizedRolesExtractor, securityUtils, barUserService);
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        this.barAuthenticationEntryPoint = barAuthenticationEntryPoint;
        this.barAccessDeniedHandler = barAccessDeniedHandler;
    }

    @Override
    @SuppressFBWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
    protected void configure(HttpSecurity http) {
       try {
           http
               .addFilterAfter(userAuthFilter, BearerTokenAuthenticationFilter.class)
               .sessionManagement().sessionCreationPolicy(STATELESS).and()
               .csrf().disable()
               .formLogin().disable()
               .logout().disable()
               .authorizeRequests()
               .antMatchers("/swagger-ui.html", "/webjars/springfox-swagger-ui/**", "/swagger-resources/**",
                   "/v2/**", "/health","/health/liveness","/health/readiness", "/payment-types", "/info").permitAll()
               .anyRequest().authenticated()
               .and()
               .oauth2ResourceServer()
               .jwt()
               .jwtAuthenticationConverter(jwtAuthenticationConverter)
               .and()
               .and()
               .oauth2Client()
               .and()
               .exceptionHandling().accessDeniedHandler(barAccessDeniedHandler)
               .authenticationEntryPoint(barAuthenticationEntryPoint);
       } catch (Exception exception) {
           LOG.info("Error in SpringSecurityConfiguration Configure: {}", exception);
       }


    }

    @Bean
    @SuppressWarnings("unchecked")
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
            JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(Arrays.asList(allowedAudiences));

        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

        // Commented issuer validation as confirmed by IDAM
        /* OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);*/
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withTimestamp,
            audienceValidator);
        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }
}
