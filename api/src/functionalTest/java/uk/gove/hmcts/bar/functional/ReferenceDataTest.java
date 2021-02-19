package uk.gove.hmcts.bar.functional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@RunWith(SpringRunner.class)
public class ReferenceDataTest extends FunctionalTest {

    @Test
    public void shouldReturnPaymentTypes() {
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .when()
            .get("/payment-types")
            .then()
            .statusCode(200);
    }

    @Test
    public void testReturnAvailableActions() {
        String token = "Bearer " +  authenticatorClient.authenticate(users.get(Roles.DELIVERY_MANAGER_Y431), password);
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .header("Authorization", token)
            .header("SiteId", Sites.Y431.name())
            .when()
            .get("/payment-action")
            .then()
            .statusCode(200);
    }


}
