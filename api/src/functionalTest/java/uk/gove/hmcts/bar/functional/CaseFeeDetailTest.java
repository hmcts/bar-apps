package uk.gove.hmcts.bar.functional;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@RunWith(SpringRunner.class)
public class CaseFeeDetailTest extends FunctionalTest {


    @Test
    public void createCaseFeeDetailWithValidSiteId() throws JSONException {
        JSONObject paymentInstructionPayload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " + authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        HashMap createdPayment = createCardPaymentInstruction(paymentInstructionPayload.toString(), token, Sites.Y431.name()).getBody().as(HashMap.class);

        JSONObject caseFeeDetailPayLoad = new JSONObject()
            .put("payment_instruction_id", createdPayment.get("id"))
            .put("amount", 550)
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

    @Test
    public void createCaseFeeDetailWithInvalidSiteId() throws JSONException {

        JSONObject paymentInstructionPayload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " + authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        HashMap createdPayment = createCardPaymentInstruction(paymentInstructionPayload.toString(), token, Sites.Y431.name()).getBody().as(HashMap.class);

        JSONObject caseFeeDetailPayLoad = new JSONObject()
            .put("payment_instruction_id", 4000000)
            .put("amount", 550)
            .put("fee_code", "FEE00007")
            .put("fee_description", "description")
            .put("fee_version", "3")
            .put("case_reference", "case1");

        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y610.name())
            .body(caseFeeDetailPayLoad.toString())
            .when()
            .post("/fees")
            .then()
            .statusCode(403);
    }



    @Test
    public void updateCaseFeeDetailWithValidSiteId() throws JSONException {
        JSONObject paymentInstructionPayload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        HashMap createdPayment = createCardPaymentInstruction(paymentInstructionPayload.toString(), token, Sites.Y431.name()).getBody().as(HashMap.class);

        JSONObject caseFeeDetailPayLoad = new JSONObject()
            .put("payment_instruction_id", createdPayment.get("id"))
            .put("amount", 550)
            .put("fee_code", "FEE00007")
            .put("fee_description", "description")
            .put("fee_version", "3")
            .put("case_reference", "case1");

        HashMap response =given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(caseFeeDetailPayLoad.toString())
            .when()
            .post("/fees").getBody().as(HashMap.class);

        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(caseFeeDetailPayLoad.toString())
            .when()
            .put("/fees/"+response.get("case_fee_id"))
            .then()
            .statusCode(200);

    }


    @Test
    public void updateCaseFeeDetailWithInvalidSiteId() throws JSONException {

        JSONObject paymentInstructionPayload = new JSONObject()
            .put("payer_name", "John Doe")
            .put("amount", 550)
            .put("status", "D")
            .put("currency", "GBP")
            .put("authorization_code", "123456");
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.FEE_CLERK_Y431), password);
        HashMap createdPayment = createCardPaymentInstruction(paymentInstructionPayload.toString(), token, Sites.Y431.name()).getBody().as(HashMap.class);

        JSONObject caseFeeDetailPayLoad = new JSONObject()
            .put("payment_instruction_id", createdPayment.get("id"))
            .put("amount", 550)
            .put("fee_code", "FEE00007")
            .put("fee_description", "description")
            .put("fee_version", "3")
            .put("case_reference", "case1");

        HashMap response =given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .body(caseFeeDetailPayLoad.toString())
            .when()
            .post("/fees").getBody().as(HashMap.class);


        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y610.name())
            .body(caseFeeDetailPayLoad.toString())
            .when()
            .put("/fees/"+response.get("case_fee_id"))
            .then()
            .statusCode(403);

    }


}
