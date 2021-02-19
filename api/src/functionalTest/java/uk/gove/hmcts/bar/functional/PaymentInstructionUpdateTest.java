package uk.gove.hmcts.bar.functional;

import io.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@RunWith(SpringRunner.class)
public class PaymentInstructionUpdateTest extends FunctionalTest {

    @Test
    public void updatePaymentInstructionWithValidSiteId() throws JSONException {
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        HashMap createdPayment = createCardPaymentInstruction(payload.toString(), token, Sites.Y431.name()).getBody().as(HashMap.class);

        payload = new JSONObject()
            .put("payer_name", "Jane Doe");
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(payload.toString())
            .when()
            .put("/cards/" + createdPayment.get("id"))
            .then()
            .statusCode(200);
    }

    @Test
    public void updatePaymentInstructionWithInvalidSiteId() throws JSONException {
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        HashMap createdPayment = createCardPaymentInstruction(payload.toString(), token, Sites.Y431.name()).getBody().as(HashMap.class);

        payload = new JSONObject()
            .put("payer_name", "Jane Doe")
            .put("status", "P");
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y610.name())
            .body(payload.toString())
            .when()
            .put("/cards/" + createdPayment.get("id"))
            .then()
            .statusCode(403);
    }

    @Test
    public void updatePaymentInstructionWithDifferentSiteId() throws JSONException {
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe Milton")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y610), password);
        HashMap createdPayment = createCardPaymentInstruction(payload.toString(), token, Sites.Y610.name()).getBody().as(HashMap.class);

        token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        payload = new JSONObject()
            .put("payer_name", "Jane Doe")
            .put("status", "P");
        Response response = given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(payload.toString())
            .when()
            .put("/cards/" + createdPayment.get("id"));
        response.then().statusCode(404);
        Assert.assertEquals("{\"message\":\"payment instruction on site " + Sites.Y431.name() + " for id=" +
            createdPayment.get("id") + " was not found\"}", response.getBody().print());
    }

    @Test
    public void submitPaymentInstructionWithValidSiteId() throws JSONException {
        // Create payment
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        HashMap createdPayment = createCardPaymentInstruction(payload.toString(), token, Sites.Y431.name()).getBody().as(HashMap.class);

        // Add fee
        addCaseFee(550, token, createdPayment.get("id").toString(), Sites.Y431.name());

        // Make changes
        payload = new JSONObject()
            .put("status", "V")
            .put("action", "Process");
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(payload.toString())
            .when()
            .put("/payment-instructions/" + createdPayment.get("id"))
            .then()
            .statusCode(200);
    }

    @Test
    public void submitPaymentInstructionWithDifferentSiteId() throws JSONException {
        // Create payment
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y610), password);
        HashMap createdPayment = createCardPaymentInstruction(payload.toString(), token, Sites.Y610.name()).getBody().as(HashMap.class);

        // Add fee
        addCaseFee(550, token, createdPayment.get("id").toString(), Sites.Y610.name());

        // Make changes
        payload = new JSONObject()
            .put("status", "V")
            .put("action", "Process");
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(payload.toString())
            .when()
            .put("/payment-instructions/" + createdPayment.get("id"))
            .then()
            .statusCode(403);
    }
}
