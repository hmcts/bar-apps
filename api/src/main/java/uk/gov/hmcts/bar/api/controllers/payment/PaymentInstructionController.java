package uk.gov.hmcts.bar.api.controllers.payment;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.collections.MultiMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.bar.api.auth.BarWrappedHttpRequest;
import uk.gov.hmcts.bar.api.data.enums.PaymentStatusEnum;
import uk.gov.hmcts.bar.api.data.exceptions.PaymentProcessException;
import uk.gov.hmcts.bar.api.data.model.*;
import uk.gov.hmcts.bar.api.data.service.*;
import uk.gov.hmcts.bar.api.data.utils.PaymentStatusEnumConverter;
import uk.gov.hmcts.bar.api.data.utils.Util;
import uk.gov.hmcts.bar.api.integration.payhub.service.PayHubService;

import javax.validation.Valid;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController

@Validated
public class PaymentInstructionController {

    private final PaymentInstructionService paymentInstructionService;

    private final CaseFeeDetailService caseFeeDetailService;

    private final UnallocatedAmountService unallocatedAmountService;

    private final BarUserService barUserService;

    private final PayHubService payHubService;

    private final FullRemissionService fullRemissionService;

    @Autowired
    public PaymentInstructionController(PaymentInstructionService paymentInstructionService,
                                        CaseFeeDetailService caseFeeDetailService,
                                        UnallocatedAmountService unallocatedAmountService,
                                        BarUserService barUserService,
                                        PayHubService payHubService,
                                        FullRemissionService fullRemissionService) {
        this.paymentInstructionService = paymentInstructionService;
        this.caseFeeDetailService = caseFeeDetailService;
        this.unallocatedAmountService = unallocatedAmountService;
        this.barUserService = barUserService;
        this.payHubService = payHubService;
        this.fullRemissionService = fullRemissionService;
    }

    @ApiOperation(value = "Get all current payment instructions", notes = "Get all current payment instructions for a given site.",
        produces = "application/json, text/csv")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return all current payment instructions"),
        @ApiResponse(code = 404, message = "Payment instructions not found"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payment-instructions")
    public List<PaymentInstruction> getPaymentInstructions(
        BarWrappedHttpRequest request,
        @RequestHeader HttpHeaders headers,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "startDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate startDate,
        @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate endDate,
        @RequestParam(name = "payerName", required = false) String payerName,
        @RequestParam(name = "chequeNumber", required = false) String chequeNumber,
        @RequestParam(name = "postalOrderNumber", required = false) String postalOrderNumber,
        @RequestParam(name = "dailySequenceId", required = false) String dailySequenceId,
        @RequestParam(name = "allPayInstructionId", required = false) String allPayInstructionId,
        @RequestParam(name = "caseReference", required = false) String caseReference,
        @RequestParam(name = "paymentType", required = false) String paymentType,
        @RequestParam(name = "action", required = false) String action,
        @RequestParam(name = "authorizationCode", required = false) String authorizationCode,
        @RequestParam(name = "oldStatus", required = false) String oldStatus,
        @RequestParam(name = "payhubReference", required = false) String payhubReference) {

        List<PaymentInstruction> paymentInstructionList = null;

        if (checkAcceptHeaderForCsv(headers)){
            paymentInstructionList =  paymentInstructionService.getAllPaymentInstructionsByTTB(startDate,endDate,request.getBarUser().getSelectedSiteId());
        } else {
            PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto =
                createPaymentInstructionCriteria(status, startDate, endDate, payerName, chequeNumber, postalOrderNumber,
                    dailySequenceId, allPayInstructionId, paymentType, action, caseReference, null, null,
                    authorizationCode, oldStatus, payhubReference);

            paymentInstructionList = paymentInstructionService
                .getAllPaymentInstructions(request.getBarUser(), paymentInstructionSearchCriteriaDto);
        }
        return Util.updateStatusAndActionDisplayValue(paymentInstructionList);
    }

    @ApiOperation(value = "Get all current payment instructions", notes = "Get all current payment instructions for a given site.",
        produces = "application/json, text/csv")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return all current payment instructions for a given user"),
        @ApiResponse(code = 404, message = "Payment instructions not found"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/{id}/payment-instructions")
    public List<PaymentInstruction> getPaymentInstructionsByIdamId (
        BarWrappedHttpRequest request,
        @PathVariable("id") String id,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "startDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate startDate,
        @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate endDate,
        @RequestParam(name = "payerName", required = false) String payerName,
        @RequestParam(name = "chequeNumber", required = false) String chequeNumber,
        @RequestParam(name = "postalOrderNumber", required = false) String postalOrderNumber,
        @RequestParam(name = "dailySequenceId", required = false) String dailySequenceId,
        @RequestParam(name = "allPayInstructionId", required = false) String allPayInstructionId,
        @RequestParam(name = "caseReference", required = false) String caseReference,
        @RequestParam(name = "paymentType", required = false) String paymentType,
        @RequestParam(name = "action", required = false) String action,
        @RequestParam(name = "piIds", required = false) String piIds,
        @RequestParam(name = "bgcNumber", required = false) String bgcNumber,
        @RequestParam(name = "oldStatus", required = false) String oldStatus,
        @RequestParam(name = "payhubReference", required = false) String payhubReference)  {

        List<PaymentInstruction> paymentInstructionList = null;

		PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = createPaymentInstructionCriteria(id,
				status, startDate, endDate, payerName, chequeNumber, postalOrderNumber, dailySequenceId,
				allPayInstructionId, paymentType, action, caseReference, piIds, bgcNumber, null, oldStatus, payhubReference);

		paymentInstructionList = paymentInstructionService
				.getAllPaymentInstructions(request.getBarUser(), paymentInstructionSearchCriteriaDto);


        return Util.updateStatusAndActionDisplayValue(paymentInstructionList);
    }

    @ApiOperation(value = "Get the payment instruction", notes = "Get the payment instruction for the given id.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return payment instruction"),
        @ApiResponse(code = 404, message = "Payment instruction not found"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payment-instructions/{id}")
    public ResponseEntity<PaymentInstruction> getPaymentInstruction(BarWrappedHttpRequest request, @PathVariable("id") Integer id) {
        PaymentInstruction paymentInstruction = paymentInstructionService.getPaymentInstruction(id, request.getBarUser().getSelectedSiteId());
        if (paymentInstruction == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(paymentInstruction, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete payment instruction", notes = "Delete payment instruction with the given id.")
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Payment instruction deleted"),
        @ApiResponse(code = 404, message = "Payment instruction not found"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/payment-instructions/{id}")
    public void deletePaymentInstruction(BarWrappedHttpRequest request, @PathVariable("id") Integer id) {
        paymentInstructionService.deletePaymentInstruction(id, request.getBarUser().getSelectedSiteId());
    }

    @ApiOperation(value = "Create card payment instruction", notes = "Create card payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Card payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cards")
    public PaymentInstruction saveCardInstruction(
        BarWrappedHttpRequest request,
        @Valid @RequestBody Card card) {
        CardPaymentInstruction cardPaymentInstruction = CardPaymentInstruction.cardPaymentInstructionWith()
            .payerName(card.getPayerName())
            .amount(card.getAmount())
            .currency(card.getCurrency())
            .status(card.getStatus())
            .authorizationCode(card.getAuthorizationCode())
            .build();
        return paymentInstructionService.createPaymentInstruction(request.getBarUser(), cardPaymentInstruction);
    }

    @ApiOperation(value = "Update card payment instruction", notes = "Update card payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Card payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/cards/{id}")
    public ResponseEntity<Void> updateCardInstruction(@PathVariable("id") Integer id,
                                                      @ApiParam(value="Card request", required=true) @Valid @RequestBody Card card,
                                                      BarWrappedHttpRequest request) {
        paymentInstructionService.updatePaymentInstruction(request.getBarUser(), id,card);
        return new ResponseEntity<>(HttpStatus.OK);
    }



    @ApiOperation(value = "Create cheque payment instruction", notes = "Create cheque payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Cheque payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cheques")
    public PaymentInstruction saveChequeInstruction(BarWrappedHttpRequest request,
                                                    @Valid @RequestBody Cheque cheque) {
        ChequePaymentInstruction chequePaymentInstruction = ChequePaymentInstruction.chequePaymentInstructionWith()
            .payerName(cheque.getPayerName())
            .amount(cheque.getAmount())
            .currency(cheque.getCurrency())
            .status(cheque.getStatus())
            .chequeNumber(cheque.getChequeNumber()).build();
        return paymentInstructionService.createPaymentInstruction(request.getBarUser(), chequePaymentInstruction);
    }

    @ApiOperation(value = "Update cheque payment instruction", notes = "Update cheque payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Postal Order payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/cheques/{id}")
    public ResponseEntity<Void> updateChequeInstruction(@PathVariable("id") Integer id,
                                                        @ApiParam(value="Cheque request",required=true) @Valid @RequestBody Cheque cheque,
                                                        BarWrappedHttpRequest request) {
        paymentInstructionService.updatePaymentInstruction(request.getBarUser(), id,cheque);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Reject the payment instruction", notes = "Reject payment instruction with the given id.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Payment instruction rejected"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/payment-instructions/{id}/reject")
	public ResponseEntity<Void> rejectPaymentInstruction(@PathVariable("id") Integer id,
                                                         BarWrappedHttpRequest request) {
		Optional<BarUser> userOptional = barUserService.getBarUser();
		BarUser user = null;
		if (userOptional.isPresent()) {
			user = userOptional.get();
		} else {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		String status = null;
		if (Util.isUserDeliveryManager(user.getRoles())) {
			status = PaymentStatusEnum.REJECTEDBYDM.dbKey();
		} else if (Util.isUserSrFeeClerk(user.getRoles())) {
			status = PaymentStatusEnum.REJECTED.dbKey();
		}
		PaymentInstructionRequest paymentInstructionRequest = PaymentInstructionRequest.paymentInstructionRequestWith()
				.status(status).build();
		paymentInstructionService.updatePaymentInstruction(request.getBarUser(), id, paymentInstructionRequest);
		return new ResponseEntity<>(HttpStatus.OK);
	}


    @ApiOperation(value = "Create cash payment instruction", notes = "Create cash payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Cash payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cash")
    public PaymentInstruction saveCashInstruction(BarWrappedHttpRequest request,
                                                  @ApiParam(value="Cash request",required=true) @Valid @RequestBody Cash cash) {
        CashPaymentInstruction cashPaymentInstruction = CashPaymentInstruction.cashPaymentInstructionWith()
            .payerName(cash.getPayerName())
            .amount(cash.getAmount())
            .status(cash.getStatus())
            .currency(cash.getCurrency()).build();
        return paymentInstructionService.createPaymentInstruction(request.getBarUser(), cashPaymentInstruction);
    }

    @ApiOperation(value = "Create remission", notes = "Create a full remission with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Full remission created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/remissions")
    public PaymentInstruction saveRemission(BarWrappedHttpRequest request,
                                            @ApiParam(value="Full remission request",required=true) @Valid @RequestBody FullRemission fullRemission) {
        FullRemissionPaymentInstruction remissionPaymentInstruction = FullRemissionPaymentInstruction.fullRemissionPaymentInstructionWith()
            .payerName(fullRemission.getPayerName())
            .remissionReference(fullRemission.getRemissionReference()).build();
        return paymentInstructionService.createPaymentInstruction(request.getBarUser(), remissionPaymentInstruction);
    }

    @ApiOperation(value = "Update full remission", notes = "Update remission instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Remission instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/remissions/{id}")
    public ResponseEntity<Void> updateRemissionInstruction(@PathVariable("id") Integer id , @ApiParam(value="Full remission request",required=true) @Valid @RequestBody FullRemission fullRemission)  {
        fullRemissionService.updateFullRemission(id, fullRemission);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "Update cash payment instruction", notes = "Update cash payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Cash payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/cash/{id}")
    public ResponseEntity<Void> updateCashInstruction(BarWrappedHttpRequest request,
                                                      @PathVariable("id") Integer id , @ApiParam(value="Cash request",required=true) @Valid @RequestBody Cash cash) {
        paymentInstructionService.updatePaymentInstruction(request.getBarUser(), id,cash);
        return new ResponseEntity<>(HttpStatus.OK);
    }




    @ApiOperation(value = "Create poatal order payment instruction", notes = "Create postal order payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Postal order payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/postal-orders")
    public PaymentInstruction savePostalOrderInstruction(
        BarWrappedHttpRequest request,
        @ApiParam(value="Postal Order request",required=true) @Valid @RequestBody PostalOrder postalOrder) {
        PostalOrderPaymentInstruction postalOrderPaymentInstruction = PostalOrderPaymentInstruction.postalOrderPaymentInstructionWith()
            .payerName(postalOrder.getPayerName())
            .amount(postalOrder.getAmount())
            .currency(postalOrder.getCurrency())
            .status(postalOrder.getStatus())
            .postalOrderNumber(postalOrder.getPostalOrderNumber()).build();
        return paymentInstructionService.createPaymentInstruction(request.getBarUser(), postalOrderPaymentInstruction);
    }

    @ApiOperation(value = "Update postal order payment instruction", notes = "Update postal order payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Postal Order payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/postal-orders/{id}")
    public ResponseEntity<Void> updatePostalOrderInstruction(BarWrappedHttpRequest request,
                                                             @PathVariable("id") Integer id , @ApiParam(value="Postal order request",required=true) @Valid @RequestBody PostalOrder postalOrder) {
        paymentInstructionService.updatePaymentInstruction(request.getBarUser(), id,postalOrder);
        return new ResponseEntity<>(HttpStatus.OK);
    }



    @ApiOperation(value = "Create allpay payment instruction", notes = "Create allpay payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "AllPay payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/allpay")
    public PaymentInstruction saveAllPayInstruction(
        BarWrappedHttpRequest request,
        @ApiParam(value="All Pay request", required=true) @Valid @RequestBody AllPay allPay) {
        AllPayPaymentInstruction allPayPaymentInstruction = AllPayPaymentInstruction.allPayPaymentInstructionWith()
            .payerName(allPay.getPayerName())
            .amount(allPay.getAmount())
            .currency(allPay.getCurrency())
            .status(allPay.getStatus())
            .allPayTransactionId(allPay.getAllPayTransactionId()).build();
        return paymentInstructionService.createPaymentInstruction(request.getBarUser(), allPayPaymentInstruction);
    }

    @ApiOperation(value = "Update allpay payment instruction", notes = "Update allpay payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Allpay payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/allpay/{id}")
    public ResponseEntity<Void> updateAllPayInstruction(BarWrappedHttpRequest request,
                                                        @PathVariable("id") Integer id , @ApiParam(value="Allpay request",required=true) @Valid @RequestBody AllPay allpay) {
        paymentInstructionService.updatePaymentInstruction(request.getBarUser(), id, allpay);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "Submit current payment instructions by post clerk", notes = "Submit current payment instructions by a post clerk.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Submit current payment instructions by post clerk"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/payment-instructions/{id}")
    public ResponseEntity<Object> submitPaymentInstructionsByPostClerk(
        BarWrappedHttpRequest request,
        @PathVariable("id") Integer id,
        @RequestBody PaymentInstructionUpdateRequest paymentInstructionUpdateRequest) {
    	ResponseEntity<Object> response;
        if (null == paymentInstructionUpdateRequest) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        PaymentInstruction submittedPaymentInstruction = null;
        try {
			submittedPaymentInstruction = paymentInstructionService.submitPaymentInstruction(request.getBarUser(), id, paymentInstructionUpdateRequest);
			response = new ResponseEntity<>(submittedPaymentInstruction, HttpStatus.OK);
		} catch (PaymentProcessException e) {
			response = new ResponseEntity<>(e, HttpStatus.BAD_REQUEST);
		}
        return response;
    }

    @ApiOperation(value = "Create case fee detail for a payment instruction", notes = "Create case fee detail for a payment instruction.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Case fee detail for a payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/fees")
    public CaseFeeDetail saveCaseFeeDetail(BarWrappedHttpRequest request,@RequestBody CaseFeeDetailRequest caseFeeDetailRequest) {
        return caseFeeDetailService.saveCaseFeeDetail(request.getBarUser(),caseFeeDetailRequest);
    }

    @ApiOperation(value = "Update case fee details", notes = "Update case fee details with the given values.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Case Fee details updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error") })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/fees/{caseFeeId}")
    public CaseFeeDetail updateCaseFeeDetail(BarWrappedHttpRequest request,@PathVariable("caseFeeId") Integer caseFeeId,
                                             @RequestBody CaseFeeDetailRequest caseFeeDetailRequest) {
        return caseFeeDetailService.updateCaseFeeDetail(request.getBarUser(),caseFeeId, caseFeeDetailRequest);
    }

    @ApiOperation(value = "Delete case fee details", notes = "Delete case fee details with the given values.")
    @ApiResponses(value = { @ApiResponse(code = 204, message = "Case Fee details deleted"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error") })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/fees/{caseFeeId}")
    public ResponseEntity<Void> deleteCaseFeeDetail(@PathVariable("caseFeeId") Integer caseFeeId) {
        caseFeeDetailService.deleteCaseFeeDetail(caseFeeId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Get the payment instruction", notes = "Get the payment instruction's unallocated amount for the given id.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return payment unallocated amount"),
        @ApiResponse(code = 404, message = "Payment instruction not found"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payment-instructions/{id}/unallocated")
    public int getUnallocatedPayment(@PathVariable("id") Integer paymentId){
        return unallocatedAmountService.calculateUnallocatedAmount(paymentId);
    }

    @ApiOperation(value = "Get the payments stats", notes = "Get the payment instruction's stats showing each User's activities.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Return payment overview stats"),
        @ApiResponse(code = 500, message = "Internal server error") })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/pi-stats")
    public MultiMap getPIStats(BarWrappedHttpRequest request,
                               @RequestParam(name = "status", required = true) PaymentStatusEnum status,
                               @RequestParam(name = "oldStatus", required = false) PaymentStatusEnum oldStatus,
                               @RequestParam(name = "sentToPayhub", required = false, defaultValue = "false") boolean sentToPayhub) {
        MultiMap resultMap = null;
        String siteId = request.getBarUser().getSelectedSiteId();
        if (oldStatus != null) {
            resultMap = paymentInstructionService.getPaymentInstructionStatsByCurrentStatusGroupedByOldStatus(status.dbKey(),
                oldStatus.dbKey(), siteId);
        } else {
            resultMap = paymentInstructionService.getPaymentInstructionStats(status.dbKey(),sentToPayhub, siteId);
        }

        return resultMap;
    }

    @ApiOperation(value = "Get the payments stats along with count", notes = "Get the payment instruction's stats showing each User's activities along with count.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Return payment overview stats with count"),
        @ApiResponse(code = 500, message = "Internal server error") })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/pi-stats/count")
    public MultiMap getPIStatsCount(BarWrappedHttpRequest request,
                                    @RequestParam(name = "status", required = true) PaymentStatusEnum status,
                                    @RequestParam(name = "startDate", required = true) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate startDate,
                                    @RequestParam(name = "endDate", required = true) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate endDate) {
        MultiMap resultMap = null;
        String siteId = request.getBarUser().getSelectedSiteId();

            resultMap = paymentInstructionService.getPaymentInstructionStatsWithCount(status.dbKey(), siteId,startDate.atStartOfDay(),endDate.atTime(LocalTime.MAX));

        return resultMap;
    }

    @ApiOperation(value = "collect payment instructions count", notes = "Collect  payment instruction count  ")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return count"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payment-instructions/count")
    public long getPaymentInstructionCount(BarWrappedHttpRequest request,
        @RequestParam(name = "userId", required =  false) String userId,
        @RequestParam(name = "status") String status,
        @RequestParam(name = "startDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate startDate,
        @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate endDate) {

        long count = 0;
        if (null == userId && null == startDate && null == endDate){
            count = paymentInstructionService.getNonResetPaymentInstructionsCount(status, request.getBarUser().getSelectedSiteId());
        }
        else if(null != startDate && null != endDate) {
            PaymentInstructionStatusCriteriaDto paymentInstructionStatusCriteriaDto =
                PaymentInstructionStatusCriteriaDto.paymentInstructionStatusCriteriaDto().status(status).userId(userId)
                    .startDate(startDate.atStartOfDay())
                    .endDate(endDate.atTime(LocalTime.MAX))
                    .siteId(request.getBarUser().getSelectedSiteId())
                    .build();
            count = paymentInstructionService.getPaymentInstructionsCount(paymentInstructionStatusCriteriaDto);
        }

        return count;
    }

    @ApiOperation(value = "collect stats for a user", notes = "Collect all payment instruction stats for a user grouped by type for a given status")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return stats for a given user"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/{id}/payment-instructions/stats")
    public EntityModel<MultiMap> getPaymentInstructionStatsByUser(
        BarWrappedHttpRequest request,
        @PathVariable("id") String id,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "old_status", required = false) Optional<String> oldStatus,
        @RequestParam(name = "sentToPayhub", required = false, defaultValue = "false") boolean sentToPayhub) {

        MultiMap stats = paymentInstructionService.getPaymentStatsByUserGroupByType(id, status, oldStatus, sentToPayhub, request.getBarUser().getSelectedSiteId());
        Link link = linkTo(methodOn(PaymentInstructionController.class).getPaymentInstructionStatsByUser(request, id, status, oldStatus, sentToPayhub)).withSelfRel();
        return new EntityModel<>(stats, link);
    }

    @ApiOperation(value = "collect stats for a user", notes = "Collect all payment instruction stats for a user grouped by action and type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return stats for a given user"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/{id}/payment-instructions/action-stats")
    public EntityModel<MultiMap> getPaymentInstructionStatsByUserGroupByAction(
        BarWrappedHttpRequest request,
        @PathVariable("id") String id,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "old_status", required = false) Optional<String> oldStatus,
        @RequestParam(name = "sentToPayhub", required = false, defaultValue = "false") boolean sentToPayhub) {

        MultiMap stats = paymentInstructionService.getPaymentInstructionsByUserGroupByActionAndType(id, status, oldStatus, sentToPayhub, request.getBarUser().getSelectedSiteId());
        Link link = linkTo(methodOn(PaymentInstructionController.class).getPaymentInstructionStatsByUserGroupByAction(request, id, status, oldStatus, sentToPayhub)).withSelfRel();
        return new EntityModel<>(stats, link);
    }


    @ApiOperation(value = "Send to payhub", notes = "Send all payment-instructions with TTB status to payhub")
    @ApiResponses(value = {@ApiResponse(code = 200, message = ""),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping({"/payment-instructions/send-to-payhub", "/payment-instructions/send-to-payhub/{timestamp}"})
	public ResponseEntity<PayHubResponseReport> sendToPayHub(@RequestHeader HttpHeaders headers,
                                                             @PathVariable(name = "timestamp", required = false)
                                                             Optional<Long> reportTimestamp,
                                                             BarWrappedHttpRequest request)  {
        String bearerToken = headers.getFirst("Authorization");
        LocalDateTime reportDate;
        if (!reportTimestamp.isPresent()) {
            reportDate = LocalDateTime.now();
        } else {
            reportDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(reportTimestamp.get()), ZoneId.of("Europe/London"));
        }
        PayHubResponseReport report = payHubService.sendPaymentInstructionToPayHub(request.getBarUser(), bearerToken, reportDate);
        return ResponseEntity.ok(report);
    }

    private PaymentInstructionSearchCriteriaDto createPaymentInstructionCriteria(
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String payerName,
        String chequeNumber,
        String postalOrderNumber,
        String dailySequenceId,
        String allPayInstructionId,
        String paymentType,
        String action,
        String caseReference,
        String multiplePiIds,
        String bgcNumber,
        String authorizationCode,
        String oldStatus,
        String payhubReference
    ){
        return createPaymentInstructionCriteria(null, status, startDate, endDate, payerName, chequeNumber,
            postalOrderNumber, dailySequenceId, allPayInstructionId, paymentType, action, caseReference, multiplePiIds, bgcNumber,
            authorizationCode, oldStatus, payhubReference);
    }

    private PaymentInstructionSearchCriteriaDto createPaymentInstructionCriteria(
        String userId,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String payerName,
        String chequeNumber,
        String postalOrderNumber,
        String dailySequenceId,
        String allPayInstructionId,
        String paymentType,
        String action,
        String caseReference,
        String multiplePiIds,
        String bgcNumber,
        String authorizationCode,
        String oldStatus,
        String payhubReference
    ){
		return PaymentInstructionSearchCriteriaDto.paymentInstructionSearchCriteriaDto().status(status).userId(userId)
				.startDate(startDate == null ? null : startDate.atStartOfDay())
				.endDate(endDate == null ? null : endDate.atTime(LocalTime.of(23, 59, 59))).payerName(payerName)
				.chequeNumber(chequeNumber).postalOrderNumer(postalOrderNumber).dailySequenceId(dailySequenceId)
				.allPayInstructionId(allPayInstructionId).paymentType(paymentType).action(action)
				.caseReference(caseReference).multiplePiIds(multiplePiIds).bgcNumber(bgcNumber)
                .authorizationCode(authorizationCode).oldStatus(oldStatus).payhubReference(payhubReference).build();
    }

    private boolean checkAcceptHeaderForCsv(HttpHeaders headers){
        return headers.getAccept().contains(new MediaType("text","csv"));
    }

    @InitBinder
	public void initBinder(final WebDataBinder webdataBinder) {
		webdataBinder.registerCustomEditor(PaymentStatusEnum.class, new PaymentStatusEnumConverter());
	}

}
