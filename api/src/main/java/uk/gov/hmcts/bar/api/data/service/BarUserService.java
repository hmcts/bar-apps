package uk.gov.hmcts.bar.api.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.bar.api.data.model.BarUser;
import uk.gov.hmcts.bar.api.data.repository.BarUserRepository;
import uk.gov.hmcts.bar.api.data.utils.Util;
import uk.gov.hmcts.bar.api.security.utils.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Optional;

@Service
@Transactional
public class BarUserService {

    private final BarUserRepository barUserRepository;
    private final CloseableHttpClient httpClient;
    private final String siteApiUrl;
    private final Cache cache;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    public BarUserService(BarUserRepository barUserRepository,
                          CloseableHttpClient httpClient,
                          @Value("${site.api.url}") String siteApiUrl,
                          CacheManager cacheManager, SecurityUtils securityUtils){
        this.barUserRepository = barUserRepository;
        this.httpClient = httpClient;
        this.siteApiUrl = siteApiUrl;
        this.cache = cacheManager.getCache("barusers");
        this.securityUtils = securityUtils;
    }

    public BarUser saveUser(@NotNull BarUser barUser) {
        BarUser existingUser = getBarUser(barUser.getId());
        if (existingUser == null || !existingUser.equals(barUser)){
            BarUser savedUser = barUserRepository.save(barUser);
            cache.put(savedUser.getId(), savedUser);
            return savedUser;
        } else {
            return existingUser;
        }
    }

    public String getCurrentUserId() {
        Optional<String> userId;
        UserInfo userInfo = securityUtils.getUserInfo();
        userId = Optional.ofNullable(userInfo.getUid());
        return userId.orElseThrow(() -> new AccessDeniedException("failed to identify user"));
	}

    public Optional<BarUser> getBarUser() {
        String id = getCurrentUserId();
        return Optional.ofNullable(getBarUser(id));
    }

    public BarUser getBarUser(String id) {
        BarUser user = cache.get(id, BarUser.class);
        if (user == null) {
            user = barUserRepository.findBarUserById(id);
            cache.put(id, user);
        }
        return user;
    }

    public Boolean validateUserAgainstSite(String email, String userToken, String siteId) throws IOException {
        if(Util.StringUtils.isAnyBlank(email, userToken, siteId)) {
            return false;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        HttpGet httpGet = new HttpGet(siteApiUrl + "/sites/" + siteId + "/users/" + email);
        httpGet.setHeader("Content-type", "application/json");
        httpGet.setHeader("Authorization", userToken);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        return objectMapper.readValue(response.getEntity().getContent(), Boolean.class);
    }

}
