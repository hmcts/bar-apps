package uk.gove.hmcts.bar.functional;

import io.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@RunWith(SpringRunner.class)
public class PaymentInstructionCreationTest extends FunctionalTest {

    @Test
    public void testCardPaymentCreationWithCorrectSiteId() throws JSONException {
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456")
            .put("site_id", Sites.Y610.name());
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        Response response = createCardPaymentInstruction(payload.toString(), token, Sites.Y431.name());
        response.then().statusCode(201);
        Assert.assertEquals(Sites.Y431.name(), response.as(HashMap.class).get("site_id"));
    }

    @Test
    public void testCardPaymentCreationWithInvalidSiteId() throws JSONException {
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        createCardPaymentInstruction(payload.toString(), token, Sites.Y610.name()).then()
            .statusCode(403);
    }
}
