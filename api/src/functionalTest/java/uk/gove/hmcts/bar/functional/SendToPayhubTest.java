package uk.gove.hmcts.bar.functional;

import io.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@RunWith(SpringRunner.class)
public class SendToPayhubTest extends FunctionalTest {

    @Test
    public void testSendPaymentToPayhub() throws JSONException {
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.DELIVERY_MANAGER_Y431), password);

        // create payment
        Map createdPayment = createPayment(token);

        // attach fee
        createFee(createdPayment, token);

        // set to TTB
        changeStatus("/cards/", "TTB", token, createdPayment);

        // send to payhub
        Response response = given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .when()
            .get("/payment-instructions/send-to-payhub");
        response.then().statusCode(200);
        Map resp = response.as(HashMap.class);
        Assert.assertEquals(1, resp.get("success"));
    }

    @Test
    public void testSendFullRemissionToPayhub() throws JSONException {
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.DELIVERY_MANAGER_Y431), password);
        // create full-remission
        Map createdFullRemission = createFullRemission(token);

        // attach fee
        createFee(createdFullRemission, token);

        // set to TTB
        changeStatus("/remissions/", "TTB", token, createdFullRemission);

        // send to payhub
        Response response = given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .when()
            .get("/payment-instructions/send-to-payhub");
        response.then().statusCode(200);
        Map resp = response.as(HashMap.class);
        Assert.assertEquals(1, resp.get("success"));
    }

    private Map createPayment(String token) throws JSONException {
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 55000)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        Response response = createCardPaymentInstruction(payload.toString(), token, Sites.Y431.name());
        response.then().statusCode(201);
        return response.as(HashMap.class);
    }

    private Map createFullRemission(String token) throws JSONException {
        JSONObject payload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("status", "D")
            .put("remission_reference", "12345678901");
        Response response = createFullRemission(payload.toString(), token, Sites.Y431.name());
        response.then().statusCode(201);
        return response.as(HashMap.class);
    }

    private void createFee(Map createdPayment, String token) throws JSONException {
        int amount = (int)createdPayment.get("amount") == 0 ? 55000 : (int) createdPayment.get("amount");
        JSONObject caseFeeDetailPayLoad = new JSONObject()
            .put("payment_instruction_id", createdPayment.get("id"))
            .put("amount", amount)
            .put("fee_code", "FEE00007")
            .put("fee_description", "description")
            .put("fee_version", "3")
            .put("case_reference", "case1");
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(caseFeeDetailPayLoad.toString())
            .when()
            .post("/fees")
            .then()
            .statusCode(201);
    }

    private void changeStatus(String endpoint, String status, String token, Map createdPayment) throws JSONException {
        JSONObject payload = new JSONObject()
            .put("status", "TTB");
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(payload.toString())
            .when()
            .put(endpoint + createdPayment.get("id"));
    }
}
