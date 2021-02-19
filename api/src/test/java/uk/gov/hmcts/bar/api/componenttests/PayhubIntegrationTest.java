package uk.gov.hmcts.bar.api.componenttests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.bar.api.componenttests.utils.DbTestUtil;

import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PayhubIntegrationTest extends ComponentTestBase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DbTestUtil.toggleSendToPayhub(getWebApplicationContext(), true);
        wireMockRule.stubFor(post(urlPathMatching("/lease"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjbWMiLCJleHAiOjE1MzMyMzc3NjN9.3iwg2cCa1_G9-TAMupqsQsIVBMWg9ORGir5xZyPhDabk09Ldk0-oQgDQq735TjDQzPI8AxL1PgjtOPDKeKyxfg[akiss@reformMgmtDevBastion02")
            )
        );

        wireMockRule.stubFor(post(urlPathMatching("/payment-records"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody("{\"reference\": \"RC-1534-8634-8352-6509\", \"date_created\": \"2018-08-21T14:58:03.630+0000\", \"status\": \"Initiated\", \"payment_group_reference\": \"2018-15348634835\"}")
            )
        );

        wireMockRule.stubFor(post(urlPathMatching("/remission"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.TEXT_PLAIN)
                .withBody("RM-1534-8634-8352-6509")
            )
        );
    }

   @Test
    public void testSendPaymentInstrucitonToPayhub() throws Exception {
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), null);
        restActionsForDM
            .get("/payment-instructions/send-to-payhub/")
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, map -> {
                Assert.assertEquals(4, map.get("total"));
                Assert.assertEquals(4, map.get("success"));
            }));
    }

    @Test
    public void testSendPaymentInstrucitonToPayhubWithReportDate() throws Exception {
        Long reportDate = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), null);
        restActionsForDM
            .get("/payment-instructions/send-to-payhub/" + reportDate)
            .andExpect(status().isOk())
            .andExpect(body().as(Map.class, map -> {
                Assert.assertEquals(4, map.get("total"));
                Assert.assertEquals(4, map.get("success"));
            }));
    }

        @Test
        public void testSendPaymentInstrucitonToPayhubWithInvalidReportDate() throws Exception {
            Long reportDate = LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();
            DbTestUtil.insertBGCNumber(getWebApplicationContext());
            DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), null);
            restActionsForDM
                .get("/payment-instructions/send-to-payhub/" + reportDate)
                .andExpect(status().isBadRequest());
        }

        @Test
        public void testSendPaymentInstrucitonToPayhub_withWrongUser() throws Exception {
            String userId = restActions.getUserInfoForRestAction().getUid();
            DbTestUtil.insertBGCNumber(getWebApplicationContext());
            DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), userId);
            restActions
                .get("/payment-instructions/send-to-payhub/")
                .andExpect(status().isForbidden());
        }

        @Test
        public void testSendPaymentInstrucitonWhenFeatureIsOff() throws Exception {
            DbTestUtil.insertBGCNumber(getWebApplicationContext());
            DbTestUtil.toggleSendToPayhub(getWebApplicationContext(), false);
            DbTestUtil.insertPaymentInstructions(getWebApplicationContext(), null);
            restActionsForDM
                .get("/payment-instructions/send-to-payhub/")
                .andExpect(status().isBadRequest())
                .andExpect(body().as(Map.class, resp -> {
                    Assert.assertEquals("This function is temporarily unavailable.\n" +
                        "Please contact support.", resp.get("message"));
                }));
        }
}
