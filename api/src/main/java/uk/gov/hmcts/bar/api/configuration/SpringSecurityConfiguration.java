package uk.gov.hmcts.bar.api.configuration;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.hmcts.reform.auth.checker.spring.useronly.AuthCheckerUserOnlyFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthCheckerUserOnlyFilter authCheckerFilter;

    @Override
    @SuppressFBWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
    protected void configure(HttpSecurity http) throws Exception {

       http
           .addFilter(authCheckerFilter)
           .sessionManagement().sessionCreationPolicy(STATELESS).and()
           .csrf().disable()
           .formLogin().disable()
           .logout().disable()
           .authorizeRequests()
           .antMatchers("/swagger-ui.html", "/webjars/springfox-swagger-ui/**", "/swagger-resources/**",
               "/v2/**", "/health","/health/liveness","/health/readiness", "/payment-types", "/info").permitAll()
           .anyRequest().authenticated();
    }

}
