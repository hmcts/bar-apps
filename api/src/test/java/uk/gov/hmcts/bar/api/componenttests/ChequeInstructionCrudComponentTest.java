package uk.gov.hmcts.bar.api.componenttests;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONParser;
import uk.gov.hmcts.bar.api.data.model.CaseFeeDetailRequest;
import uk.gov.hmcts.bar.api.data.model.Cheque;
import uk.gov.hmcts.bar.api.data.model.ChequePaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.bar.api.data.model.Cheque.chequePaymentInstructionRequestWith;
import static uk.gov.hmcts.bar.api.data.model.ChequePaymentInstruction.chequePaymentInstructionWith;
import static uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith;
public class ChequeInstructionCrudComponentTest extends ComponentTestBase {


    @Test
    public void whenChequeInstructionDetails_thenCreateChequePaymentInstruction() throws Exception {
        Cheque proposedChequePaymentInstructionRequest =chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        restActions
            .post("/cheques", proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated())
            .andExpect(body().as(ChequePaymentInstruction.class, chequePaymentInstruction -> {
                assertThat(chequePaymentInstruction).isEqualToComparingOnlyGivenFields(
                    chequePaymentInstructionWith()
                        .payerName("Mr Payer Payer")
                        .amount(500)
                        .currency("GBP")
                        .chequeNumber("000000"));
            }));
    }

    @Test
    public void whenChequeInstructionWithInvalidChequeNumber_thenReturn400() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("xxxxxx").build();

        restActions
            .post("/cheques", proposedChequePaymentInstructionRequest)
            .andExpect(status().isBadRequest())
        ;
    }

    @Test
    public void whenChequeInstructionWithInvalidCurrency_thenReturn400() throws Exception {
        Cheque proposedChequePaymentInstructionRequest =chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("xxx")
            .chequeNumber("000000").build();

        restActions
            .post("/cheques", proposedChequePaymentInstructionRequest)
            .andExpect(status().isBadRequest())
        ;
    }



    @Test
    public void givenChequePaymentInstructionDetails_retrieveThem() throws Exception {
        Cheque proposedChequePaymentInstructionRequest =chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        ChequePaymentInstruction retrievedChequePaymentInstruction =chequePaymentInstructionWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();


        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, (chequesList) -> {
                assertThat(chequesList.get(0).equals(retrievedChequePaymentInstruction));
            }));

    }

    @Test
    public void givenChequePaymentInstructionDetails_retrieveOneOfThem() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").chequeNumber("000000").status("D").build();

        restActions.post("/cheques", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

        restActions.get("/payment-instructions/1").andExpect(status().isOk())
            .andExpect(body().as(ChequePaymentInstruction.class, (pi) -> {
                assertThat(pi.getAmount() == 500);
            }));
    }

    @Test
    public void givenChequePaymentInstructionDetails_retrieveOneOfThemWithWrongId() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").chequeNumber("000000").status("D").build();

        restActions.post("/cheques", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

        restActions.get("/payment-instructions/2").andExpect(status().isNotFound());
    }

    @Test
    public void whenChequePaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .delete("/payment-instructions/1")
            .andExpect(status().isNoContent());


    }

    @Test
    public void whenNonExistingChequePaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        Cheque proposedChequePaymentInstructionRequest =chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .delete("/payment-instructions/1000")
            .andExpect(status().isNotFound());


    }


    @Test
    public void whenChequePaymentInstructionIsSubmittedByPostClerk_expectStatus_200() throws Exception {
        Cheque proposedChequePaymentInstructionRequest =chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        PaymentInstructionUpdateRequest statusUpdateRequest= paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/payment-instructions/1",statusUpdateRequest)
            .andExpect(status().isOk());


    }

    @Test
    public void whenNonExistingChequePaymentInstructionIsSubmittedByPostClerk_expectStatus_404() throws Exception {
        Cheque proposedChequePaymentInstructionRequest =chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        PaymentInstructionUpdateRequest statusUpdateRequest= paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/payment-instructions/1000", statusUpdateRequest)
            .andExpect(status().isNotFound());


    }

    @Test
    public void whenCaseReferenceForAChequePaymentInstructionIsCreated_expectStatus_201() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .caseReference("case102")
            .feeCode("X001")
            .amount(200)
            .feeVersion("1")
            .paymentInstructionId(1)
            .build();

        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees",caseFeeDetailRequest)
            .andExpect(status().isCreated());


    }


    @Test
    public void whenInvalidCaseReferenceForAChequePaymentInstructionIsCreated_expectStatus_201() throws Exception {
        Cheque proposedChequePaymentInstructionRequest =chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .paymentInstructionId(1)
            .caseReference("??????????")
            .build();

        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees",caseFeeDetailRequest)
            .andExpect(status().isBadRequest());


    }

    @Test
    public void whenSearchChequePaymentInstructionByPayerName_expectStatus_200() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        restActions
            .post("/cheques", proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .get("/payment-instructions?payerName=Mr Payer Payer")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, chequePaymentInstructionList -> {
                assertThat(chequePaymentInstructionList.get(0)).isEqualToComparingOnlyGivenFields(
                    chequePaymentInstructionWith()
                        .payerName("Mr Updated Payer")
                        .amount(600)
                        .currency("GBP")
                        .status("D")
                        .chequeNumber("000000"));
            }));
    }




    @Test
    public void whenSearchNonExistingChequePaymentInstructionByPayerName_expectStatus_200AndEmptyList() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        restActions
            .post("/cheques", proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .get("/payment-instructions?payerName=NonExisting")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, chequePaymentInstructionList-> assertTrue(chequePaymentInstructionList.isEmpty())));
    }

    @Test
    public void whenSearchChequePaymentInstructionByChequeNumber_expectStatus_200() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        restActions
            .post("/cheques", proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .get("/payment-instructions?chequeNumber=000000")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, chequePaymentInstructionList -> {
                assertThat(chequePaymentInstructionList.get(0)).isEqualToComparingOnlyGivenFields(
                    chequePaymentInstructionWith()
                        .payerName("Mr Updated Payer")
                        .amount(600)
                        .currency("GBP")
                        .status("D")
                        .chequeNumber("000000"));
            }));
    }


    @Test
    public void whenSearchNonExistingChequePaymentInstructionByChequeNumber_expectStatus_200AndEmptyList() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();

        restActions
            .post("/cheques", proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .get("/payment-instructions?chequeNumber=111111")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, chequePaymentInstructionList-> assertTrue(chequePaymentInstructionList.isEmpty())));
    }

    @Test
    public void whenChequePaymentInstructionIsUpdated_expectStatus_200() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();


        Cheque updatedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(6000)
            .currency("GBP")
            .status("P")
            .chequeNumber("000000").build();


        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cheques/1",updatedChequePaymentInstructionRequest)
            .andExpect(status().isOk());

    }

    @Test
    public void whenBgcNumberIsProvidedOnUpdate_expectedToBeSaved() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();


        Cheque updatedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(6000)
            .currency("GBP")
            .status("P")
            .chequeNumber("000000").bgcNumber("12345").build();


        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cheques/1",updatedChequePaymentInstructionRequest)
            .andExpect(status().isOk());

        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, (allPayList) -> {
                String bgcNumber = (String)((Map)allPayList.get(0)).get("bgc_number");
                assertThat(bgcNumber).isEqualTo("12345");
            }));
    }

    @Test
    public void whenNonExistingChequePaymentInstructionIsUpdated_expectStatus_404() throws Exception {
        Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();


        Cheque updatedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(6000)
            .currency("GBP")
            .chequeNumber("000000").status("D").build();


        restActions
            .post("/cheques",  proposedChequePaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cheques/1000",updatedChequePaymentInstructionRequest)
            .andExpect(status().isNotFound());

    }

	@Test
	public void whenChequePaymentInstructionSubmittedToSrFeeClerkByFeeClerk_expectThePIToAppearInSrFeeClerkOverview()
			throws Exception {
		Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").chequeNumber("000000").build();

		restActions.post("/allpay", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cheque modifiedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("PR").chequeNumber("000000").build();

		restActionsForFeeClerk.put("/allpay/1", modifiedChequePaymentInstructionRequest).andExpect(status().isOk());

		String jsonResponse = restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8")).andReturn().getResponse()
				.getContentAsString();
		JSONObject feeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
				.get("fee-clerk")).get(0);
		assertEquals( "fee-clerk-fn fee-clerk-ln",feeClerk.get("bar_user_full_name"));
		assertEquals( 1,feeClerk.get("count_of_payment_instruction_in_specified_status"));
	}

	@Test
	public void whenChequePaymentInstructionSubmittedToDMBySrFeeClerk_expectThePIToAppearDMOverview() throws Exception {
		Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").chequeNumber("000000").build();

		restActions.post("/allpay", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cheque pendingApprovedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("PR").chequeNumber("000000").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedChequePaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Cheque approvedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("R").chequeNumber("000000").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedChequePaymentInstructionRequest).andExpect(status().isOk());

		String jsonResponse = restActionsForDM.get("/users/pi-stats?status=R").andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8")).andReturn().getResponse()
				.getContentAsString();
		JSONObject srFeeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
				.get("sr-fee-clerk")).get(0);
		assertEquals( "sr-fee-clerk-fn sr-fee-clerk-ln",srFeeClerk.get("bar_user_full_name"));
		assertEquals( 1,srFeeClerk.get("count_of_payment_instruction_in_specified_status"));
	}

	@Test
	public void whenChequePaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIStatusAsRDM()
			throws Exception {
		Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").chequeNumber("000000").build();

		restActions.post("/allpay", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cheque pendingApprovedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("PR").chequeNumber("000000").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedChequePaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Cheque approvedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("R").chequeNumber("000000").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedChequePaymentInstructionRequest).andExpect(status().isOk());

		Cheque rejectedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("RDM").chequeNumber("000000").build();

		restActionsForDM.patch("/payment-instructions/1/reject", rejectedChequePaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/payment-instructions/1").andExpect(status().isOk())
				.andExpect(body().as(ChequePaymentInstruction.class, (pi) -> {
					assertThat(pi.getStatus().equals("RDM"));
				}));
	}

	@Test
	public void whenChequePaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIInSrFeeClerkOverviewStats()
			throws Exception {
		Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").chequeNumber("000000").build();

		restActions.post("/allpay", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cheque pendingApprovedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("PR").chequeNumber("000000").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedChequePaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Cheque approvedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("R").chequeNumber("000000").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedChequePaymentInstructionRequest).andExpect(status().isOk());

		Cheque rejectedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("RDM").chequeNumber("000000").build();

		restActionsForDM.patch("/payment-instructions/1/reject", rejectedChequePaymentInstructionRequest)
				.andExpect(status().isOk());

		String jsonResponse = restActionsForSrFeeClerk.get("/users/pi-stats?status=RDM&oldStatus=R")
				.andExpect(status().isOk()).andExpect(content().contentType("application/json;charset=UTF-8"))
				.andReturn().getResponse().getContentAsString();
		System.out.println(jsonResponse);
		JSONObject srFeeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
				.get("sr-fee-clerk")).get(0);
		assertEquals( "sr-fee-clerk-fn sr-fee-clerk-ln",srFeeClerk.get("bar_user_full_name"));
		assertEquals( 1,srFeeClerk.get("count_of_payment_instruction_in_specified_status"));
	}

	@Test
	public void whenQueriedWithAListOfPaymentInstructionIds_receiveAllThePaymentInstructionsInTheQueryList()
			throws Exception {
		Cheque proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(500).currency("GBP").chequeNumber("000000").status("D").build();
		ChequePaymentInstruction retrievedChequePaymentInstruction = chequePaymentInstructionWith()
				.payerName("Mr Payer Payer").amount(500).currency("GBP").chequeNumber("000000").status("D").build();

		restActions.post("/cheques", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

		proposedChequePaymentInstructionRequest = chequePaymentInstructionRequestWith().payerName("Mr Payer2 Payer2")
				.amount(500).currency("GBP").chequeNumber("000000").status("D").build();
		ChequePaymentInstruction retrievedChequePaymentInstruction2 = chequePaymentInstructionWith()
				.payerName("Mr Payer2 Payer2").amount(500).currency("GBP").chequeNumber("000000").status("D").build();

		restActions.post("/cheques", proposedChequePaymentInstructionRequest).andExpect(status().isCreated());

		restActionsForSrFeeClerk.get("/users/2/payment-instructions?piIds=1,2").andExpect(status().isOk())
				.andExpect(body().as(List.class, (chequePayList) -> {
					assertThat(chequePayList.get(0).equals(retrievedChequePaymentInstruction));
					assertThat(chequePayList.get(1).equals(retrievedChequePaymentInstruction2));
				}));
	}

}
