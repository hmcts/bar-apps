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
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext());

        restActions
            .get("/users/1234/payment-instructions/stats?status=TTB")
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, item -> {
                assertTrue(item.size() == 2);
                Map stats = ((ArrayList<Map>)((Map)item.get("content")).get("0")).get(0);
                assertNull(stats.get("bgc"));
                assertEquals(1, stats.get("count"));
                assertEquals("CARD", stats.get("payment_type"));
            }));
    }

    @Test
    public void testGettingPaymentInstructionActionStats() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext());

        restActions
            .get("/users/1234/payment-instructions/action-stats?status=TTB")
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, item -> {
                assertTrue(item.size() == 2);
                Map stats = ((ArrayList<Map>)((Map)item.get("content")).get("0")).get(0);
                assertNull(stats.get("bgc"));
                assertEquals(1, stats.get("count"));
                assertEquals("CARD", stats.get("payment_type"));
            }));
    }

    @Test
    public void testGettingPaymentInstructionActionStatsForRejectedItems() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext());

        restActions
            .get("/users/1234/payment-instructions/action-stats?status=RDM&old_status=R")
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, item -> {
                assertTrue(item.size() == 2);
                Map stats = ((ArrayList<Map>)((Map)item.get("content")).get("0")).get(0);
                assertNull(stats.get("bgc"));
                assertEquals(1, stats.get("count"));
                assertEquals("CHEQUE", stats.get("payment_type"));
            }));
    }
}
