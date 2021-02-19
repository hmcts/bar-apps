package uk.gov.hmcts.bar.api.controllers;

import org.junit.Test;
import uk.gov.hmcts.bar.api.componenttests.ComponentTestBase;
import uk.gov.hmcts.bar.api.componenttests.utils.DbTestUtil;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentInstructionStatsTest extends ComponentTestBase {

    @Test
    public void testGettingPaymentInstructionStats() throws Exception {

        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), restActions.getUserInfoForRestAction().getUid());

        restActions
            .get("/users/" + restActions.getUserInfoForRestAction().getUid() + "/payment-instructions/stats?status=TTB")
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, item -> {
                assertTrue(item.size() == 4);
                Map stats = ((ArrayList<Map>) ((Map) item.get("content")).get("0")).get(0);
                assertNull(stats.get("bgc"));
                assertEquals(1, stats.get("count"));
                assertEquals("CARD", stats.get("payment_type"));
            }));
    }

    @Test
    public void testGettingPaymentInstructionActionStats() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), restActions.getUserInfoForRestAction().getUid());

        restActions
            .get("/users/" + restActions.getUserInfoForRestAction().getUid() + "/payment-instructions/action-stats?status=TTB")
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, item -> {
                assertTrue(item.size() == 4);
                Map stats = ((ArrayList<Map>) ((Map) item.get("content")).get("0")).get(0);
                assertNull(stats.get("bgc"));
                assertEquals(1, stats.get("count"));
                assertEquals("CARD", stats.get("payment_type"));
            }));
    }

    @Test
    public void testGettingPaymentInstructionActionStatsForRejectedItems() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), restActions.getUserInfoForRestAction().getUid());

        restActions
            .get("/users/" + restActions.getUserInfoForRestAction().getUid() + "/payment-instructions/action-stats?status=RDM&old_status=A")
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, item -> {
                assertTrue(item.size() == 3);
                Map stats = ((ArrayList<Map>) ((Map) item.get("content")).get("0")).get(0);
                assertNull(stats.get("bgc"));
                assertEquals(1, stats.get("count"));
                assertEquals("CHEQUE", stats.get("payment_type"));
            }));
    }
}
