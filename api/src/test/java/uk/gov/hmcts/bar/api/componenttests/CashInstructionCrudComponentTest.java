package uk.gov.hmcts.bar.api.componenttests;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONParser;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.bar.api.data.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.bar.api.data.model.Cash.cashPaymentInstructionRequestWith;
import static uk.gov.hmcts.bar.api.data.model.CashPaymentInstruction.cashPaymentInstructionWith;
import static uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith;

public class CashInstructionCrudComponentTest extends ComponentTestBase {


    @Test
    public void whenCashPaymentInstructionDetails_thenCreateCashPaymentInstruction() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        restActions
            .post("/cash", proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated())
            .andExpect(body().as(CashPaymentInstruction.class, cashPaymentInstruction -> {
                assertThat(cashPaymentInstruction).isEqualToComparingOnlyGivenFields(
                    cashPaymentInstructionWith()
                        .payerName("Mr Payer Payer")
                        .amount(500)
                        .currency("GBP").status("D"));
            }));

    }

    @Test
    public void whenCashPaymentInstructionWithInvalidCurrency_thenReturn400() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("XXX").build();

        restActions
            .post("/cash", proposedCashPaymentInstructionRequest)
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenCashPaymentInstructionDetails_retrieveThem() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        CashPaymentInstruction  retrievedCashPaymentInstruction = cashPaymentInstructionWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, (cashList) -> {
                assertThat(cashList.get(0).equals(retrievedCashPaymentInstruction));
            }));


    }

    @Test
    public void givenCashPaymentInstructionDetails_retrieveOneOfThem() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").status("D").build();

        restActions.post("/cash", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

        restActions.get("/payment-instructions/1").andExpect(status().isOk())
            .andExpect(body().as(CashPaymentInstruction.class, (pi) -> {
                assertThat(pi.getAmount() == 500);
            }));
    }

    @Test
    public void givenCashPaymentInstructionDetails_retrieveOneOfThemWithWrongId() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").status("D").build();

        restActions.post("/cash", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

        restActions.get("/payment-instructions/2").andExpect(status().isNotFound());
    }

    @Test
    public void whenCashPaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        Cash proposedCashPaymentInstructionRequest =cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .delete("/payment-instructions/1")
            .andExpect(status().isNoContent());


    }


    @Test
    public void whenNonExistingCashPaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .delete("/payment-instructions/1000")
            .andExpect(status().isNotFound());


    }


    @Test
    public void whenCashPaymentInstructionIsSubmittedByPostClerk_expectStatus_200() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();


        PaymentInstructionUpdateRequest request= paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .put("/payment-instructions/1",request)
            .andExpect(status().isOk());


    }


    @Test
    public void whenNonExistingCashPaymentInstructionIsSubmittedByPostClerk_expectStatus_404() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();


        PaymentInstructionUpdateRequest request= paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .put("/payment-instructions/1000",request)
            .andExpect(status().isNotFound());


    }

    @Test
    public void whenCaseReferenceForACashPaymentInstructionIsCreated_expectStatus_201() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .paymentInstructionId(1)
            .caseReference("case102")
            .feeCode("X001")
            .amount(200)
            .feeVersion("1")
            .build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees",caseFeeDetailRequest)
            .andExpect(status().isCreated());


    }

    @Test
    public void whenCaseReferenceForACashPaymentInstructionIsUpdated_expectStatus_404() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .paymentInstructionId(1)
            .caseReference("case102")
            .feeCode("X001")
            .amount(200)
            .feeVersion("1")
            .build();

        CaseFeeDetailRequest updatedCaseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .paymentInstructionId(100)
            .caseReference("case103")
            .feeCode("X001")
            .amount(200)
            .feeVersion("1")
            .build();


        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());


        MvcResult resultOfPost = restActions
            .post("/fees",caseFeeDetailRequest)
            .andReturn();

        CaseFeeDetail createdcaseFeeDetail = getObjectMapper().readValue(resultOfPost.getResponse().getContentAsString(),CaseFeeDetail.class);

        MvcResult resultOfPut = restActions
            .put("/fees/"+createdcaseFeeDetail.getCaseFeeId(),updatedCaseFeeDetailRequest)
            .andExpect(status().isNotFound())
            .andReturn();

    }

    @Test
    public void whenInvalidCaseReferenceForACashPaymentInstructionIsCreated_expectStatus_400() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
            .paymentInstructionId(1)
            .caseReference("????????")
            .build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees",caseFeeDetailRequest)
            .andExpect(status().isBadRequest());


    }


    @Test
    public void whenSearchCashPaymentInstructionByPayerName_expectStatus_200() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions?payerName=Mr Payer Payer")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, cashPaymentInstructionList -> {
                assertThat(cashPaymentInstructionList.get(0)).isEqualToComparingOnlyGivenFields(
                    cashPaymentInstructionWith()
                        .payerName("Mr Payer Payer")
                        .amount(500)
                        .currency("GBP").status("D"));
            }));


    }
    @Test
    public void whenSearchNonExistingCashPaymentInstructionByPayerName_expectStatus_200AndEmptyList() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions?payerName=NonExisting")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, cashPaymentInstructionList-> assertTrue(cashPaymentInstructionList.isEmpty())));

    }


    @Test
    public void whenCashPaymentInstructionIsUpdated_expectStatus_200() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        Cash updatedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Updated Payer")
            .amount(6000)
            .currency("GBP").status("D").build();


        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cash/1",updatedCashPaymentInstructionRequest)
            .andExpect(status().isOk());

    }

    @Test
    public void whenBgcNumberIsProvidedOnUpdate_expectedToBeSaved() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        Cash updatedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Updated Payer")
            .amount(6000)
            .currency("GBP").status("D").bgcNumber("12345").build();


        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cash/1",updatedCashPaymentInstructionRequest)
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
    public void whenNonExistingCashPaymentInstructionIsUpdated_expectStatus_404() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        Cash updatedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Updated Payer")
            .amount(6000)
            .currency("GBP").status("D").build();


        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cash/1000",updatedCashPaymentInstructionRequest)
            .andExpect(status().isNotFound());

    }


	@Test
	public void whenCashPaymentInstructionSubmittedToSrFeeClerkByFeeClerk_expectThePIToAppearInSrFeeClerkOverview()
			throws Exception {
		Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("D").build();

		restActions.post("/allpay", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cash modifiedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("PR").build();

		restActionsForFeeClerk.put("/allpay/1", modifiedCashPaymentInstructionRequest).andExpect(status().isOk());

		String jsonResponse = restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8")).andReturn().getResponse()
				.getContentAsString();
		JSONObject feeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
				.get("fee-clerk")).get(0);
		assertEquals( "fee-clerk-fn fee-clerk-ln",feeClerk.get("bar_user_full_name"));
		assertEquals( 1,feeClerk.get("count_of_payment_instruction_in_specified_status"));
	}

	@Test
	public void whenCashPaymentInstructionSubmittedToDMBySrFeeClerk_expectThePIToAppearDMOverview() throws Exception {
		Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("D").build();

		restActions.post("/allpay", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cash pendingApprovedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("PR").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedCashPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Cash approvedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("R").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedCashPaymentInstructionRequest).andExpect(status().isOk());

		String jsonResponse = restActionsForDM.get("/users/pi-stats?status=R").andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8")).andReturn().getResponse()
				.getContentAsString();
		JSONObject srFeeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
				.get("sr-fee-clerk")).get(0);
		assertEquals( "sr-fee-clerk-fn sr-fee-clerk-ln",srFeeClerk.get("bar_user_full_name"));
		assertEquals( 1,srFeeClerk.get("count_of_payment_instruction_in_specified_status"));
	}

	@Test
	public void whenCashPaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIStatusAsRDM()
			throws Exception {
		Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("D").build();

		restActions.post("/allpay", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cash pendingApprovedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("PR").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedCashPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Cash approvedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("R").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedCashPaymentInstructionRequest).andExpect(status().isOk());

		Cash rejectedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("RDM").build();

		restActionsForDM.patch("/payment-instructions/1/reject", rejectedCashPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/payment-instructions/1").andExpect(status().isOk())
				.andExpect(body().as(CashPaymentInstruction.class, (pi) -> {
					assertThat(pi.getStatus().equals("RDM"));
				}));
	}

	@Test
	public void whenCashPaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIInSrFeeClerkOverviewStats()
			throws Exception {
		Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("D").build();

		restActions.post("/allpay", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Cash pendingApprovedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("PR").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedCashPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Cash approvedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("R").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedCashPaymentInstructionRequest).andExpect(status().isOk());

		Cash rejectedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(550).currency("GBP").status("RDM").build();

		restActionsForDM.patch("/payment-instructions/1/reject", rejectedCashPaymentInstructionRequest)
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
		Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer Payer")
				.amount(500).currency("GBP").status("D").build();
		CashPaymentInstruction retrievedCashPaymentInstruction = cashPaymentInstructionWith()
				.payerName("Mr Payer Payer").amount(500).currency("GBP").status("D").build();

		restActions.post("/cash", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

		proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith().payerName("Mr Payer2 Payer2")
				.amount(500).currency("GBP").status("D").build();
		CashPaymentInstruction retrievedCashPaymentInstruction2 = cashPaymentInstructionWith()
				.payerName("Mr Payer2 Payer2").amount(500).currency("GBP").status("D").build();

		restActions.post("/cash", proposedCashPaymentInstructionRequest).andExpect(status().isCreated());

		restActionsForSrFeeClerk.get("/users/2/payment-instructions?piIds=1,2").andExpect(status().isOk())
				.andExpect(body().as(List.class, (cashPayList) -> {
					assertThat(cashPayList.get(0).equals(retrievedCashPaymentInstruction));
					assertThat(cashPayList.get(1).equals(retrievedCashPaymentInstruction2));
				}));
	}

    @Test
    public void givenCashPIsSubmitted_getTheirCount() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        Cash  validatedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("V").build();

        Cash  submittedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("PR").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());
        restActions
            .put("/cash/1", validatedCashPaymentInstructionRequest)
            .andExpect(status().isOk());
        restActions
            .put("/cash/1", submittedCashPaymentInstructionRequest)
            .andExpect(status().isOk());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyy");
        String startDate = LocalDate.now().format(dtf);
        String endDate = LocalDate.now().format(dtf);
        restActionsForFeeClerk.get("/payment-instructions/count?status=PR&userId=1234&startDate="+startDate+"&endDate="+endDate).andExpect(status().isOk())
            .andExpect(body().as(Long.class, (count) -> {
                assertThat(count.equals(1));
            }));
    }
    @Test
    public void givenCashPIsSubmitted_getResetCount() throws Exception {
        Cash proposedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").build();

        Cash  validatedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("V").build();

        Cash  submittedCashPaymentInstructionRequest = cashPaymentInstructionRequestWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("PR").build();

        restActions
            .post("/cash",  proposedCashPaymentInstructionRequest)
            .andExpect(status().isCreated());
        restActions
            .put("/cash/1", validatedCashPaymentInstructionRequest)
            .andExpect(status().isOk());
        restActions
            .put("/cash/1", submittedCashPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActionsForFeeClerk.get("/payment-instructions/count?status=PR").andExpect(status().isOk())
            .andExpect(body().as(Long.class, (count) -> {
                assertThat(count.equals(1));
            }));
    }

}
