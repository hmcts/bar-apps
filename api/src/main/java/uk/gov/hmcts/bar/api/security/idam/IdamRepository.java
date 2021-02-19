package uk.gov.hmcts.bar.api.security.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

/**
 * Repository class to connect to idam api
 */
@Component
public class IdamRepository {

    private final IdamClient idamClient;

    @Autowired
    public IdamRepository(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    @Cacheable("userInfoCache")
    public UserInfo getUserInfo(String jwtToken) {
        return idamClient.getUserInfo("Bearer " + jwtToken);
    }
}
