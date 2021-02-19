package uk.gov.hmcts.bar.api.componenttests;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONParser;
import uk.gov.hmcts.bar.api.componenttests.utils.DbTestUtil;
import uk.gov.hmcts.bar.api.data.model.CaseFeeDetailRequest;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest;
import uk.gov.hmcts.bar.api.data.model.PostalOrder;
import uk.gov.hmcts.bar.api.data.model.PostalOrderPaymentInstruction;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith;
import static uk.gov.hmcts.bar.api.data.model.PostalOrder.postalOrderPaymentInstructionRequestWith;
import static uk.gov.hmcts.bar.api.data.model.PostalOrderPaymentInstruction.postalOrderPaymentInstructionWith;

public class PostalOrderCrudComponentTest extends ComponentTestBase {

    @Test
    public void whenPostalOrderPaymentInstructionDetails_thenCreatePostalOrderPaymentInstruction() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated())
            .andExpect(body().as(PostalOrderPaymentInstruction.class, postalOrderPaymentInstruction -> {
                assertThat(postalOrderPaymentInstruction).isEqualToComparingOnlyGivenFields(
                    postalOrderPaymentInstructionRequestWith()
                        .payerName("Mr Payer Payer")
                        .amount(500)
                        .currency("GBP").status("D")
                        .postalOrderNumber("000000"));
            }));
    }

    @Test
    public void whenPostalOrderInstructionWithInvalidCurrency_thenReturn400() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("xxx")
            .postalOrderNumber("000000").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isBadRequest())
        ;
    }


    @Test
    public void whenPostalOrderInstructionWithInvalidPostalOrderNumber_thenReturn400() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("xxxxxx").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isBadRequest())
        ;
    }


    @Test
    public void givenPostalOrderPaymentInstructionDetails_retrieveThem() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();

        PostalOrderPaymentInstruction retrievedPostalOrderPaymentInstruction = postalOrderPaymentInstructionWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, (postalOrdersList) -> {
                assertThat(postalOrdersList.get(0).equals(retrievedPostalOrderPaymentInstruction));
            }));

    }

    @Test
    public void givenPostalOrderPaymentInstructionDetails_retrieveOneOfThem() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").postalOrderNumber("000000").status("D").build();

        restActions.post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions.get("/payment-instructions/1").andExpect(status().isOk())
            .andExpect(body().as(PostalOrderPaymentInstruction.class, (pi) -> {
                assertThat(pi.getAmount() == 500);
            }));
    }

    @Test
    public void givenPostalOrderPaymentInstructionDetails_retrieveOneOfThemWithWrongId() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").postalOrderNumber("000000").status("D").build();

        restActions.post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions.get("/payment-instructions/2").andExpect(status().isNotFound());
    }

    @Test
    public void whenPostalOrderPaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .delete("/payment-instructions/1")
            .andExpect(status().isNoContent());


    }

    @Test
    public void whenNonExistingPostalOrderPaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .delete("/payment-instructions/1000")
            .andExpect(status().isNotFound());


    }

    @Test
    public void whenPostalOrderPaymentInstructionIsSubmitted_expectStatus_200() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();


        PaymentInstructionUpdateRequest stattusUpdateRequest = paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/payment-instructions/1", stattusUpdateRequest)
            .andExpect(status().isOk());


    }

    @Test
    public void whenBgcNumberIsProvidedOnUpdate_expectedToBeSaved() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        PostalOrder updatedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(6000)
            .currency("GBP")
            .status("P")
            .postalOrderNumber("000000").bgcNumber("12345").build();


        restActions
            .post("/cheques", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cheques/1", updatedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, (allPayList) -> {
                String bgcNumber = (String) ((Map) allPayList.get(0)).get("bgc_number");
                assertThat(bgcNumber).isEqualTo("12345");
            }));
    }

    @Test
    public void whenNonExistingPostalOrderPaymentInstructionIsUpdated_expectStatus_404() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();


        PaymentInstructionUpdateRequest statusUpdateRequest = paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/payment-instructions/1000", statusUpdateRequest)
            .andExpect(status().isNotFound());


    }

    @Test
    public void whenCaseReferenceForAChequePaymentInstructionIsCreated_expectStatus_201() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .paymentInstructionId(1)
            .caseReference("case102")
            .feeCode("X001")
            .amount(200)
            .feeVersion("1")
            .build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees", caseFeeDetailRequest)
            .andExpect(status().isCreated());


    }


    @Test
    public void whenInvalidCaseReferenceForAChequePaymentInstructionIsCreated_expectStatus_201() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .paymentInstructionId(1)
            .caseReference("$$$$$$$$$")
            .build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees", caseFeeDetailRequest)
            .andExpect(status().isBadRequest());


    }


    @Test
    public void whenSearchPostalOrderPaymentInstructionByPayerName_expectStatus_200() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .get("/payment-instructions?payerName=Mr Payer Payer")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, postalOrderPaymentInstructionList -> {
                assertThat(postalOrderPaymentInstructionList.get(0)).isEqualToComparingOnlyGivenFields(
                    postalOrderPaymentInstructionWith()
                        .payerName("Mr Updated Payer")
                        .amount(600)
                        .currency("GBP")
                        .status("D")
                        .postalOrderNumber("000000"));
            }));

    }

    @Test
    public void whenSearchNonExistingPostalOrderPaymentInstructionByPayerName_expectStatus_200AndEmptyList() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .postalOrderNumber("000000").status("D").build();

        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .get("/payment-instructions?payerName=NonExisting")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, postalOrderPaymentInstructionList -> assertTrue(postalOrderPaymentInstructionList.isEmpty())));

    }

    @Test
    public void whenPostalOrderPaymentInstructionIsSubmittedByPostClerk_expectStatus_200() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();


        PostalOrder updatedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Updated Payer")
            .amount(6000)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();


        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/postal-orders/1", updatedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

    }

    @Test
    public void whenNonExistingPostalOrderPaymentInstructionIsSubmittedByPostClerk_expectStatus_404() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();

        PostalOrder updatedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Updated Payer")
            .amount(6000)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();


        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/postal-orders/1000", updatedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isNotFound());

    }

    @Test
    public void whenPostalOrderPaymentInstructionSubmittedToSrFeeClerkByFeeClerk_expectThePIToAppearInSrFeeClerkOverview()
        throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").postalOrderNumber("000000")
            .build();

        restActions.post("/allpay", proposedPostalOrderPaymentInstructionRequest).andExpect(status().isCreated());

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
        		.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

        restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

        PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
            .build();

        restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

        PostalOrder modifiedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("PA").postalOrderNumber("000000")
            .build();

        restActionsForFeeClerk.put("/allpay/1", modifiedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        String jsonResponse = restActionsForSrFeeClerk.get("/users/pi-stats?status=PA").andExpect(status().isOk())
            .andExpect(content().contentType("application/json")).andReturn().getResponse()
            .getContentAsString();
        UserInfo userInfoForFeeClerk = restActionsForFeeClerk.getUserInfoForRestAction();
        JSONObject feeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
            .get(userInfoForFeeClerk.getUid())).get(0);
        assertEquals(userInfoForFeeClerk.getGivenName() + " " + userInfoForFeeClerk.getFamilyName(), feeClerk.get("bar_user_full_name"));
        assertEquals(1, feeClerk.get("count_of_payment_instruction_in_specified_status"));
    }

    @Test
    public void whenPostalOrderPaymentInstructionSubmittedToDMBySrFeeClerk_expectThePIToAppearDMOverview()
        throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").postalOrderNumber("000000")
            .build();

        restActions.post("/allpay", proposedPostalOrderPaymentInstructionRequest).andExpect(status().isCreated());

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
        		.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

        restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

        PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
            .build();

        restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

        PostalOrder pendingApprovedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("PA").postalOrderNumber("000000")
            .build();

        restActionsForFeeClerk.put("/allpay/1", pendingApprovedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActionsForSrFeeClerk.get("/users/pi-stats?status=PA").andExpect(status().isOk());

        PostalOrder approvedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("A").postalOrderNumber("000000")
            .build();

        restActionsForSrFeeClerk.put("/allpay/1", approvedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        String jsonResponse = restActionsForDM.get("/users/pi-stats?status=A").andExpect(status().isOk())
            .andExpect(content().contentType("application/json")).andReturn().getResponse()
            .getContentAsString();
        UserInfo userInfoSrFeeClerk = restActionsForSrFeeClerk.getUserInfoForRestAction();
        JSONObject srFeeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
            .get(userInfoSrFeeClerk.getUid())).get(0);
        assertEquals(userInfoSrFeeClerk.getGivenName() + " " + userInfoSrFeeClerk.getFamilyName(), srFeeClerk.get("bar_user_full_name"));
        assertEquals(1, srFeeClerk.get("count_of_payment_instruction_in_specified_status"));
    }

    @Test
    public void whenPostalOrderPaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIStatusAsRDM()
        throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").postalOrderNumber("000000")
            .build();

        restActions.post("/allpay", proposedPostalOrderPaymentInstructionRequest).andExpect(status().isCreated());

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
        		.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

        restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

        PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
            .build();

        restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

        PostalOrder pendingApprovedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("PA").postalOrderNumber("000000")
            .build();

        restActionsForFeeClerk.put("/allpay/1", pendingApprovedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActionsForSrFeeClerk.get("/users/pi-stats?status=PA").andExpect(status().isOk());

        PostalOrder approvedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("A").postalOrderNumber("000000")
            .build();

        restActionsForSrFeeClerk.put("/allpay/1", approvedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        PostalOrder rejectedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("RDM").postalOrderNumber("000000")
            .build();

        restActionsForDM.patch("/payment-instructions/1/reject", rejectedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActionsForSrFeeClerk.get("/payment-instructions/1").andExpect(status().isOk())
            .andExpect(body().as(PostalOrderPaymentInstruction.class, (pi) -> {
                assertThat(pi.getStatus().equals("RDM"));
            }));
    }

    @Test
    public void whenPostalOrderPaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIInSrFeeClerkOverviewStats()
        throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").postalOrderNumber("000000")
            .build();

        restActions.post("/allpay", proposedPostalOrderPaymentInstructionRequest).andExpect(status().isCreated());

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
        		.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

        restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

        PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
            .build();

        restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

        PostalOrder pendingApprovedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("PA").postalOrderNumber("000000")
            .build();

        restActionsForFeeClerk.put("/allpay/1", pendingApprovedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActionsForSrFeeClerk.get("/users/pi-stats?status=PA").andExpect(status().isOk());

        PostalOrder approvedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("A").postalOrderNumber("000000")
            .build();

        restActionsForSrFeeClerk.put("/allpay/1", approvedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        PostalOrder rejectedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(550).currency("GBP").status("RDM").postalOrderNumber("000000")
            .build();

        restActionsForDM.patch("/payment-instructions/1/reject", rejectedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        String jsonResponse = restActionsForSrFeeClerk.get("/users/pi-stats?status=RDM&oldStatus=A")
            .andExpect(status().isOk()).andExpect(content().contentType("application/json"))
            .andReturn().getResponse().getContentAsString();
        System.out.println(jsonResponse);
        UserInfo userInfoSrFeeClerk = restActionsForSrFeeClerk.getUserInfoForRestAction();
        JSONObject srFeeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
            .get(userInfoSrFeeClerk.getUid())).get(0);
        assertEquals(userInfoSrFeeClerk.getGivenName() + " " + userInfoSrFeeClerk.getFamilyName(), srFeeClerk.get("bar_user_full_name"));
        assertEquals(1, srFeeClerk.get("count_of_payment_instruction_in_specified_status"));
    }

    @Test
    public void whenQueriedWithAListOfPaymentInstructionIds_receiveAllThePaymentInstructionsInTheQueryList()
        throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").postalOrderNumber("000000").status("D")
            .build();
        PostalOrderPaymentInstruction retrievedPostalOrderPaymentInstruction = postalOrderPaymentInstructionWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").postalOrderNumber("000000").status("D")
            .build();

        restActions.post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer2 Payer2").amount(500).currency("GBP").postalOrderNumber("000000").status("D")
            .build();
        PostalOrderPaymentInstruction retrievedPostalOrderPaymentInstruction2 = postalOrderPaymentInstructionWith()
            .payerName("Mr Payer2 Payer2").amount(500).currency("GBP").postalOrderNumber("000000").status("D")
            .build();

        restActions.post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActionsForSrFeeClerk.get("/users/2/payment-instructions?piIds=1,2").andExpect(status().isOk())
            .andExpect(body().as(List.class, (postalOrderPayList) -> {
                assertThat(postalOrderPayList.get(0).equals(retrievedPostalOrderPaymentInstruction));
                assertThat(postalOrderPayList.get(1).equals(retrievedPostalOrderPaymentInstruction2));
            }));
    }

    @Test
    public void givenPostalOrderPaymentInstructionDetails_retrievePIStatsForDeliveryManagers() throws Exception {

        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructionForPIStats(getWebApplicationContext());


        String jsonResponse = restActions
            .get("/users/pi-stats?status=TTB")
            .andExpect(status().isOk()).andExpect(content().contentType("application/json"))
            .andReturn().getResponse().getContentAsString();
        System.out.println(jsonResponse);
        JSONObject deliveryManager = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
            .get("1234")).get(0);

        assertEquals(1,deliveryManager.get("count_of_payment_instruction_in_specified_status"));

    }

    @Test
    public void givenPostalOrderPaymentInstructionDetails_retrievePIStatsCountForDeliveryManagers() throws Exception {

        DbTestUtil.insertBGCNumber(getWebApplicationContext());
        DbTestUtil.insertPaymentInstructionForPIStats(getWebApplicationContext());
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(533)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();


        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyy");
        String startDate = LocalDate.now().format(dtf);
        String endDate = LocalDate.now().format(dtf);
        String jsonResponse = restActions
            .get("/users/pi-stats/count?startDate=" + startDate + "&endDate=" + endDate + "&status=D")
            .andExpect(status().isOk()).andExpect(content().contentType("application/json"))
            .andReturn().getResponse().getContentAsString();
        UserInfo userInfoBarPostClerk = restActions.getUserInfoForRestAction();
        JSONObject seniorFeeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
            .get(userInfoBarPostClerk.getUid())).get(0);

        assertEquals(1, seniorFeeClerk.get("count_of_payment_instruction_in_specified_status"));
        assertEquals("bar-post-clerk", seniorFeeClerk.get("bar_user_role"));
    }

    @Test
    public void givenPostalOrderPIsSubmitted_getTheirCount() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(533)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();

        PostalOrder validatedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(533)
            .currency("GBP").status("V")
            .postalOrderNumber("000000").build();

        PostalOrder submittedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(533)
            .currency("GBP").status("PA")
            .postalOrderNumber("000000").build();


        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());
        restActions
            .put("/postal-orders/1", validatedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());
        restActions
            .put("/postal-orders/1", submittedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyy");
        String startDate = LocalDate.now().format(dtf);
        String endDate = LocalDate.now().format(dtf);
        restActionsForFeeClerk.get("/payment-instructions/count?status=PA&userId=1234&startDate="+startDate+"&endDate="+endDate).andExpect(status().isOk())
            .andExpect(body().as(Long.class, (count) -> {
                assertThat(count.equals(1));
            }));
    }


    @Test
    public void givenPostalOrderPIsSubmitted_getNonResetCount() throws Exception {
        PostalOrder proposedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(533)
            .currency("GBP").status("D")
            .postalOrderNumber("000000").build();

        PostalOrder validatedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(533)
            .currency("GBP").status("V")
            .postalOrderNumber("000000").build();

        PostalOrder submittedPostalOrderPaymentInstructionRequest = postalOrderPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(533)
            .currency("GBP").status("PA")
            .postalOrderNumber("000000").build();


        restActions
            .post("/postal-orders", proposedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isCreated());
        restActions
            .put("/postal-orders/1", validatedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());
        restActions
            .put("/postal-orders/1", submittedPostalOrderPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActionsForFeeClerk.get("/payment-instructions/count?status=PA").andExpect(status().isOk())
            .andExpect(body().as(Long.class, (count) -> {
                assertThat(count.equals(1));
            }));
    }

}



