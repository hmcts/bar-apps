package uk.gov.hmcts.bar.api.security.validator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class AudienceValidatorTest {


    @Autowired
    AudienceValidator audienceValidator;
    List<String> validAudienceList = new ArrayList<>();

    @Before
    public void setup(){
        validAudienceList.add("bar_frontend");
        audienceValidator = new AudienceValidator(validAudienceList);
    }

    @Test
    public void testValidateAudience(){
        Jwt jwt = Mockito.mock(Jwt.class);
        Mockito.when(jwt.getAudience()).thenReturn(validAudienceList);
        OAuth2TokenValidatorResult result = audienceValidator.validate(jwt);
        Assert.assertFalse(result.hasErrors());
    }

    @Test
    public void testInValidateAudience(){
        Jwt jwt = Mockito.mock(Jwt.class);
        List<String> inValidAudienceList = new ArrayList<>();
        inValidAudienceList.add("Invalid");

        Mockito.when(jwt.getAudience()).thenReturn(inValidAudienceList);
        OAuth2TokenValidatorResult result = audienceValidator.validate(jwt);
        Assert.assertTrue(result.hasErrors());
    }

}
