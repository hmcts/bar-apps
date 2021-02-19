package uk.gov.hmcts.bar.api.componenttests;

import org.junit.Test;
import uk.gov.hmcts.bar.api.componenttests.utils.DbTestUtil;
import uk.gov.hmcts.bar.api.data.model.CardPaymentInstruction;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentInstructionUserFilterTest extends ComponentTestBase {


    @Test
    public void whenNoUsersInPath_thenGetAllPaymentInstructions() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), null);
        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().asListOf(CardPaymentInstruction.class, paymentInstructions -> {
                assertTrue(paymentInstructions.size() == 5);
            }));

    }

    @Test
    public void whenUsersInPath_thenFilterPaymentInstructionResultsByUser() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), restActions.getUserInfoForRestAction().getUid());
        restActions
            .get("/users/" + restActions.getUserInfoForRestAction().getUid() + "/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().asListOf(CardPaymentInstruction.class, paymentInstructions -> {
                assertTrue(paymentInstructions.size() == 3);
            }));
    }

    @Test
    public void testGettingPaymentInstructionsForUserByAction() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), restActions.getUserInfoForRestAction().getUid());

        restActions
            .get("/users/" + restActions.getUserInfoForRestAction().getUid() + "/payment-instructions?action=Process")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, item -> {
                assertTrue(item.size() == 3);
            }));

        restActions
            .get("/users/4321/payment-instructions?action=Process")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, item -> {
                assertTrue(item.size() == 0);
            }));
    }

    @Test
    public void testGettingPaymentInstructionsForUserByActionAndBgcNumber() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), null);

        restActions
            .get("/users/4321/payment-instructions?action=Process&bgcNumber=isNull")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, item -> {
                assertTrue(item.size() == 0);
            }));

        restActions
            .get("/users/4321/payment-instructions?action=Process&bgcNumber=123456")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, item -> {
                assertTrue(item.size() == 0);
            }));
    }
}
