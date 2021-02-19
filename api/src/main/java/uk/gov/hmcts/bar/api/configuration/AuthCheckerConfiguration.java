package uk.gov.hmcts.bar.api.configuration;

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.bar.api.auth.UserResolver;
import uk.gov.hmcts.bar.api.auth.UserTokenDetails;
import uk.gov.hmcts.bar.api.data.service.BarUserService;
import uk.gov.hmcts.reform.auth.checker.core.CachingSubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.core.user.UserRequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.spring.AuthCheckerProperties;
import uk.gov.hmcts.reform.auth.parser.idam.core.user.token.HttpComponentsBasedUserTokenParser;
import uk.gov.hmcts.reform.auth.parser.idam.core.user.token.UserTokenParser;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

@Configuration
public class AuthCheckerConfiguration {

    @Bean
    public Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor() {
        return (any) -> Collections.unmodifiableList(Arrays.asList("super", "bar-fee-clerk", "bar-senior-clerk", "bar-delivery-manager", "bar-post-clerk"));
    }

    @Bean
    public Function<HttpServletRequest, Optional<String>> userIdExtractor() {
        return (request) -> Optional.empty();
    }

    @Bean
    public UserTokenParser<UserTokenDetails> fullUserTokenParser(HttpClient userTokenParserHttpClient,
                                                                 @Value("${auth.idam.client.baseUrl}") String baseUrl) {
        return new HttpComponentsBasedUserTokenParser<>(userTokenParserHttpClient, baseUrl, UserTokenDetails.class);
    }

    @Bean
    public SubjectResolver<User> userResolver(UserTokenParser<UserTokenDetails> fullUserTokenParser, AuthCheckerProperties properties, BarUserService userService) {
        return new CachingSubjectResolver<>(new UserResolver(fullUserTokenParser, userService), properties.getUser().getTtlInSeconds(), properties.getUser().getMaximumSize());
    }

    @Bean
    public UserRequestAuthorizer<User> userRequestAuthorizer(SubjectResolver<User> userResolver,
                                                                     Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                                                     Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor) {
        return new UserRequestAuthorizer<>(userResolver, userIdExtractor, authorizedRolesExtractor);
    }

}
