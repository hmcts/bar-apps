package uk.gov.hmcts.bar.api.componenttests;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONParser;
import uk.gov.hmcts.bar.api.data.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.bar.api.data.model.Card.cardWith;
import static uk.gov.hmcts.bar.api.data.model.CardPaymentInstruction.cardPaymentInstructionWith;
import static uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith;

public class CardInstructionCrudComponentTest extends ComponentTestBase  {
    @Test
    public void whenCardPaymentInstructionDetails_thenCreateCardPaymentInstruction() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP").status("D").authorizationCode("123456").build();

        restActions
            .post("/cards", proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated())
            .andExpect(body().as(CardPaymentInstruction.class, cardPaymentInstruction -> {
                assertThat(cardPaymentInstruction).isEqualToComparingOnlyGivenFields(
                    cardPaymentInstructionWith()
                        .payerName("Mr Payer Payer")
                        .amount(500)
                        .status("D").authorizationCode("123456")
                        .currency("GBP"));
            }));

    }

    @Test
    public void whenCardPaymentInstructionWithInvalidCurrency_thenReturn400() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .status("D").authorizationCode("qwerty")
            .currency("XXX").build();

        restActions
            .post("/cards", proposedCardPaymentInstructionRequest)
            .andExpect(status().isBadRequest());
    }

    @Test
    public void whenCardPaymentInstructionWithInvalidAuthorizationCode_thenReturn400() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .status("D").authorizationCode("qwertyxxxx")
            .currency("GBP").build();

        restActions
            .post("/cards", proposedCardPaymentInstructionRequest)
            .andExpect(status().isBadRequest());
    }


    @Test
    public void givenCardPaymentInstructionDetails_retrieveThem() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .status("D").authorizationCode("qwerty")
            .currency("GBP").build();

        CardPaymentInstruction  expectedCardPaymentInstruction = cardPaymentInstructionWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .status("D").authorizationCode("qwerty")
            .currency("GBP").build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, (cardList) -> {
                assertThat(cardList.get(0).equals(expectedCardPaymentInstruction));
            }));


    }

    @Test
    public void givenCardPaymentInstructionDetails_retrieveOneOfThem() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").status("D").authorizationCode("qwerty").build();

        restActions.post("/cards", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

        restActions.get("/payment-instructions/1").andExpect(status().isOk())
            .andExpect(body().as(CardPaymentInstruction.class, (pi) -> {
                assertThat(pi.getAmount() == 500);
            }));
    }

    @Test
    public void givenCardPaymentInstructionDetails_retrieveOneOfThemWithWrongId() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer").amount(500).currency("GBP").status("D").authorizationCode("qwerty").build();

        restActions.post("/cards", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

        restActions.get("/payment-instructions/2").andExpect(status().isNotFound());
    }

    @Test
    public void whenCardPaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        Card proposedCardPaymentInstructionRequest =cardWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .currency("GBP")
            .status("D").authorizationCode("qwerty")
            .build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .delete("/payment-instructions/1")
            .andExpect(status().isNoContent());


    }


    @Test
    public void whenNonExistingCardPaymentInstructionIsDeleted_expectStatus_204() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500)
            .status("D").authorizationCode("qwerty")
            .currency("GBP").build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .delete("/payment-instructions/1000")
            .andExpect(status().isNotFound());


    }


    @Test
    public void whenCardPaymentInstructionIsSubmittedByPostClerk_expectStatus_200() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();


        PaymentInstructionUpdateRequest request= paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .put("/payment-instructions/1",request)
            .andExpect(status().isOk());


    }


    @Test
    public void whenNonExistingCardPaymentInstructionIsSubmittedByPostClerk_expectStatus_404() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("123456")
            .currency("GBP").status("D").build();


        PaymentInstructionUpdateRequest request= paymentInstructionUpdateRequestWith()
            .status("P").build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .put("/payment-instructions/1000",request)
            .andExpect(status().isNotFound());


    }

    @Test
    public void whenCaseReferenceForACardPaymentInstructionIsCreated_expectStatus_201() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
        	.paymentInstructionId(1)
            .caseReference("case102")
            .feeCode("X001")
            .amount(200)
            .feeVersion("1")
            .build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees",caseFeeDetailRequest)
            .andExpect(status().isCreated());


    }
    @Test
    public void whenInvalidCaseReferenceForACardPaymentInstructionIsCreated_expectStatus_201() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();

        CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
        	.paymentInstructionId(1)
            .caseReference("?????????")
            .build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());


        restActions
            .post("/fees",caseFeeDetailRequest)
            .andExpect(status().isBadRequest());


    }


    @Test
    public void whenSearchCardPaymentInstructionByPayerName_expectStatus_200() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions?payerName=Mr Payer Payer")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, cardPaymentInstructionList -> {
                assertThat(cardPaymentInstructionList.get(0)).isEqualToComparingOnlyGivenFields(
                    cardPaymentInstructionWith()
                        .payerName("Mr Payer Payer")
                        .amount(500)
                        .currency("GBP"));
            }));


    }
    @Test
    public void whenSearchNonExistingCardPaymentInstructionByPayerName_expectStatus_200AndEmptyList() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();

        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .get("/payment-instructions?payerName=NonExisting")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, cardPaymentInstructionList-> assertTrue(cardPaymentInstructionList.isEmpty())));

    }


    @Test
    public void whenCardPaymentInstructionIsUpdated_expectStatus_200() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();

        Card updatedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Updated Payer")
            .amount(6000).authorizationCode("qwerty")
            .currency("GBP").status("D").build();


        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cards/1",updatedCardPaymentInstructionRequest)
            .andExpect(status().isOk());

    }

    @Test
    public void whenBgcNumberIsProvidedOnUpdate_expectedToBeSaved() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();

        Cash updatedCardPaymentInstructionRequest = Cash.cashPaymentInstructionRequestWith()
            .payerName("Mr Updated Payer")
            .amount(6000)
            .currency("GBP").status("D").bgcNumber("12345").build();


        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cards/1",updatedCardPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActions
            .get("/payment-instructions")
            .andExpect(status().isOk())
            .andExpect(body().as(List.class, (allPayList) -> {
                String bgcNumber = (String)((Map)allPayList.get(0)).get("bgc_number");
                int amount = (Integer)((Map)allPayList.get(0)).get("amount");
                assertNull(bgcNumber);
                assertEquals(6000, amount);
            }));
    }
    @Test
    public void whenNonExistingCardPaymentInstructionIsUpdated_expectStatus_404() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(500).authorizationCode("qwerty")
            .currency("GBP").status("D").build();

        Card updatedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Updated Payer")
            .amount(6000).authorizationCode("qwerty")
            .currency("GBP").status("D").build();


        restActions
            .post("/cards",  proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cards/1000",updatedCardPaymentInstructionRequest)
            .andExpect(status().isNotFound());

    }

	@Test
	public void whenCardPaymentInstructionSubmittedToSrFeeClerkByFeeClerk_expectThePIToAppearInSrFeeClerkOverview()
			throws Exception {
		Card proposedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("D").authorizationCode("qwerty").build();

		restActions.post("/allpay", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Card modifiedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("PR").authorizationCode("qwerty").build();

		restActionsForFeeClerk.put("/allpay/1", modifiedCardPaymentInstructionRequest).andExpect(status().isOk());

		String jsonResponse = restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8")).andReturn().getResponse()
				.getContentAsString();
		JSONObject feeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
				.get("fee-clerk")).get(0);
		assertEquals( "fee-clerk-fn fee-clerk-ln",feeClerk.get("bar_user_full_name"));
		assertEquals( 1,feeClerk.get("count_of_payment_instruction_in_specified_status"));
	}

	@Test
	public void whenCardPaymentInstructionSubmittedToDMBySrFeeClerk_expectThePIToAppearDMOverview() throws Exception {
		Card proposedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("D").authorizationCode("qwerty").build();

		restActions.post("/allpay", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Card pendingApprovedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550)
				.currency("GBP").status("PR").authorizationCode("qwerty").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedCardPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Card approvedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("R").authorizationCode("qwerty").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedCardPaymentInstructionRequest).andExpect(status().isOk());

		String jsonResponse = restActionsForDM.get("/users/pi-stats?status=R").andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8")).andReturn().getResponse()
				.getContentAsString();
		JSONObject srFeeClerk = (JSONObject) ((JSONArray) ((JSONObject) JSONParser.parseJSON(jsonResponse))
				.get("sr-fee-clerk")).get(0);
		assertEquals( "sr-fee-clerk-fn sr-fee-clerk-ln",srFeeClerk.get("bar_user_full_name"));
		assertEquals( 1,srFeeClerk.get("count_of_payment_instruction_in_specified_status"));
	}

	@Test
	public void whenCardPaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIStatusAsRDM()
			throws Exception {
		Card proposedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("D").authorizationCode("qwerty").build();

		restActions.post("/allpay", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(500).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case103").feeCode("X003").amount(50).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Card pendingApprovedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550)
				.currency("GBP").status("PR").authorizationCode("qwerty").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedCardPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Card approvedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("R").authorizationCode("qwerty").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedCardPaymentInstructionRequest).andExpect(status().isOk());

		Card rejectedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("RDM").authorizationCode("qwerty").build();

		restActionsForDM.patch("/payment-instructions/1/reject", rejectedCardPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/payment-instructions/1").andExpect(status().isOk())
				.andExpect(body().as(CardPaymentInstruction.class, (pi) -> {
					assertThat(pi.getStatus().equals("RDM"));
				}));
	}

	@Test
	public void whenCardPaymentInstructionSubmittedBySrFeeClerkIsRejectedByDM_expectThePIInSrFeeClerkOverviewStats()
			throws Exception {
		Card proposedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("D").authorizationCode("qwerty").build();

		restActions.post("/allpay", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

		CaseFeeDetailRequest caseFeeDetailRequest = CaseFeeDetailRequest.caseFeeDetailRequestWith()
				.paymentInstructionId(1).caseReference("case102").feeCode("X001").amount(550).feeVersion("1").build();

		restActionsForFeeClerk.post("/fees", caseFeeDetailRequest).andExpect(status().isCreated());

		PaymentInstructionUpdateRequest request = paymentInstructionUpdateRequestWith().status("V").action("Process")
				.build();

		restActionsForFeeClerk.put("/payment-instructions/1", request).andExpect(status().isOk());

		Card pendingApprovedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550)
				.currency("GBP").status("PR").authorizationCode("qwerty").build();

		restActionsForFeeClerk.put("/allpay/1", pendingApprovedCardPaymentInstructionRequest)
				.andExpect(status().isOk());

		restActionsForSrFeeClerk.get("/users/pi-stats?status=PR").andExpect(status().isOk());

		Card approvedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("R").authorizationCode("qwerty").build();

		restActionsForSrFeeClerk.put("/allpay/1", approvedCardPaymentInstructionRequest).andExpect(status().isOk());

		Card rejectedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("RDM").authorizationCode("qwerty").build();

		restActionsForDM.patch("/payment-instructions/1/reject", rejectedCardPaymentInstructionRequest)
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
		Card proposedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("D").authorizationCode("qwerty").build();
		CardPaymentInstruction retrievedCardPaymentInstruction = cardPaymentInstructionWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").authorizationCode("qwerty")
				.build();

		restActions.post("/cards", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

		proposedCardPaymentInstructionRequest = cardWith().payerName("Mr Payer Payer").amount(550).currency("GBP")
				.status("D").authorizationCode("qwerty").build();
		CardPaymentInstruction retrievedCardPaymentInstruction2 = cardPaymentInstructionWith()
				.payerName("Mr Payer Payer").amount(550).currency("GBP").status("D").authorizationCode("qwerty")
				.build();

		restActions.post("/cards", proposedCardPaymentInstructionRequest).andExpect(status().isCreated());

		restActionsForSrFeeClerk.get("/users/2/payment-instructions?piIds=1,2").andExpect(status().isOk())
				.andExpect(body().as(List.class, (cardPayList) -> {
					assertThat(cardPayList.get(0).equals(retrievedCardPaymentInstruction));
					assertThat(cardPayList.get(1).equals(retrievedCardPaymentInstruction2));
				}));
	}

    @Test
    public void givenCardPIsSubmitted_getTheirCount() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(600)
            .currency("GBP").status("D")
            .authorizationCode("000000").build();

        Card validatedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(600)
            .currency("GBP").status("V")
            .authorizationCode("000000").build();

        Card submittedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(600)
            .currency("GBP").status("PR")
            .authorizationCode("000000").build();


        restActions
            .post("/cards", proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cards/1", validatedCardPaymentInstructionRequest)
            .andExpect(status().isOk());
        restActions
            .put("/cards/1", submittedCardPaymentInstructionRequest)
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
    public void givenCardPIsSubmitted_getNonResetCount() throws Exception {
        Card proposedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(600)
            .currency("GBP").status("D")
            .authorizationCode("000000").build();

        Card validatedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(600)
            .currency("GBP").status("V")
            .authorizationCode("000000").build();

        Card submittedCardPaymentInstructionRequest = cardWith()
            .payerName("Mr Payer Payer")
            .amount(600)
            .currency("GBP").status("PR")
            .authorizationCode("000000").build();


        restActions
            .post("/cards", proposedCardPaymentInstructionRequest)
            .andExpect(status().isCreated());

        restActions
            .put("/cards/1", validatedCardPaymentInstructionRequest)
            .andExpect(status().isOk());
        restActions
            .put("/cards/1", submittedCardPaymentInstructionRequest)
            .andExpect(status().isOk());

        restActionsForFeeClerk.get("/payment-instructions/count?status=PR").andExpect(status().isOk())
            .andExpect(body().as(Long.class, (count) -> {
                assertThat(count.equals(1));
            }));
    }

}
