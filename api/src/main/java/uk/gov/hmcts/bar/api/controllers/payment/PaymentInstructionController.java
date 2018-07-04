package uk.gov.hmcts.bar.api.controllers.payment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import uk.gov.hmcts.bar.api.data.enums.PaymentStatusEnum;
import uk.gov.hmcts.bar.api.data.model.AllPay;
import uk.gov.hmcts.bar.api.data.model.AllPayPaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.BarUser;
import uk.gov.hmcts.bar.api.data.model.Card;
import uk.gov.hmcts.bar.api.data.model.CardPaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.CaseFeeDetail;
import uk.gov.hmcts.bar.api.data.model.CaseFeeDetailRequest;
import uk.gov.hmcts.bar.api.data.model.Cash;
import uk.gov.hmcts.bar.api.data.model.CashPaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.Cheque;
import uk.gov.hmcts.bar.api.data.model.ChequePaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.PaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionRequest;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionSearchCriteriaDto;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest;
import uk.gov.hmcts.bar.api.data.model.PostalOrder;
import uk.gov.hmcts.bar.api.data.model.PostalOrderPaymentInstruction;
import uk.gov.hmcts.bar.api.data.service.BarUserService;
import uk.gov.hmcts.bar.api.data.service.CaseFeeDetailService;
import uk.gov.hmcts.bar.api.data.service.PaymentInstructionService;
import uk.gov.hmcts.bar.api.data.service.UnallocatedAmountService;
import uk.gov.hmcts.bar.api.data.utils.PaymentStatusEnumConverter;
import uk.gov.hmcts.bar.api.data.utils.Util;

@RestController

@Validated
public class PaymentInstructionController {

    private final PaymentInstructionService paymentInstructionService;

    private final CaseFeeDetailService caseFeeDetailService;

    private final UnallocatedAmountService unallocatedAmountService;

    private final BarUserService barUserService;

    @Autowired
    public PaymentInstructionController(PaymentInstructionService paymentInstructionService,
                                        CaseFeeDetailService caseFeeDetailService,
                                        UnallocatedAmountService unallocatedAmountService,
                                        BarUserService barUserService) {
        this.paymentInstructionService = paymentInstructionService;
        this.caseFeeDetailService = caseFeeDetailService;
        this.unallocatedAmountService = unallocatedAmountService;
        this.barUserService = barUserService;
    }

    @ApiOperation(value = "Get all current payment instructions", notes = "Get all current payment instructions for a given site.",
        produces = "application/json, text/csv")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return all current payment instructions"),
        @ApiResponse(code = 404, message = "Payment instructions not found"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payment-instructions")
    public List<PaymentInstruction> getPaymentInstructions(@RequestHeader HttpHeaders headers,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "startDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate startDate,
        @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate endDate,
        @RequestParam(name = "payerName", required = false) String payerName,
        @RequestParam(name = "chequeNumber", required = false) String chequeNumber,
        @RequestParam(name = "postalOrderNumber", required = false) String postalOrderNumber,
        @RequestParam(name = "dailySequenceId", required = false) Integer dailySequenceId,
        @RequestParam(name = "allPayInstructionId", required = false) String allPayInstructionId,
        @RequestParam(name = "caseReference", required = false) String caseReference,
        @RequestParam(name = "paymentType", required = false) String paymentType,
        @RequestParam(name = "action", required = false) String action) {

        List<PaymentInstruction> paymentInstructionList = null;

        if (checkAcceptHeaderForCsv(headers)){
            paymentInstructionList =  paymentInstructionService.getAllPaymentInstructionsByTTB(startDate,endDate);
        } else {
            PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto =
                createPaymentInstructionCriteria(status, startDate, endDate, payerName, chequeNumber, postalOrderNumber,
                    dailySequenceId, allPayInstructionId, paymentType, action, caseReference);

            paymentInstructionList = paymentInstructionService
                .getAllPaymentInstructions(paymentInstructionSearchCriteriaDto);
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
    public List<PaymentInstruction> getPaymentInstructionsByIdamId(
        @PathVariable("id") String id,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "startDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate startDate,
        @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "ddMMyyyy") LocalDate endDate,
        @RequestParam(name = "payerName", required = false) String payerName,
        @RequestParam(name = "chequeNumber", required = false) String chequeNumber,
        @RequestParam(name = "postalOrderNumber", required = false) String postalOrderNumber,
        @RequestParam(name = "dailySequenceId", required = false) Integer dailySequenceId,
        @RequestParam(name = "allPayInstructionId", required = false) String allPayInstructionId,
        @RequestParam(name = "caseReference", required = false) String caseReference,
        @RequestParam(name = "paymentType", required = false) String paymentType,
        @RequestParam(name = "action", required = false) String action) {

        List<PaymentInstruction> paymentInstructionList = null;


        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto =
            createPaymentInstructionCriteria(id, status, startDate, endDate, payerName, chequeNumber, postalOrderNumber,
                dailySequenceId, allPayInstructionId, paymentType, action, caseReference);

        paymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(paymentInstructionSearchCriteriaDto);

        return Util.updateStatusAndActionDisplayValue(paymentInstructionList);
    }

    @ApiOperation(value = "Get the payment instruction", notes = "Get the payment instruction for the given id.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return payment instruction"),
        @ApiResponse(code = 404, message = "Payment instruction not found"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/payment-instructions/{id}")
    public ResponseEntity<PaymentInstruction> getPaymentInstruction(@PathVariable("id") Integer id) {
        PaymentInstruction paymentInstruction = paymentInstructionService.getPaymentInstruction(id);
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
    public void deletePaymentInstruction(@PathVariable("id") Integer id) {
        paymentInstructionService.deletePaymentInstruction(id);
    }

    @ApiOperation(value = "Create card payment instruction", notes = "Create card payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Card payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cards")
    public PaymentInstruction saveCardInstruction(
        @Valid @RequestBody Card card) {
        CardPaymentInstruction cardPaymentInstruction = CardPaymentInstruction.cardPaymentInstructionWith()
            .payerName(card.getPayerName())
            .amount(card.getAmount())
            .currency(card.getCurrency())
            .status(card.getStatus())
            .authorizationCode(card.getAuthorizationCode())
            .build();
        return paymentInstructionService.createPaymentInstruction(cardPaymentInstruction);
    }

    @ApiOperation(value = "Update card payment instruction", notes = "Update card payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Card payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/cards/{id}")
    public ResponseEntity<Void> updateCardInstruction(@PathVariable("id") Integer id , @ApiParam(value="Card request",required=true) @Valid @RequestBody Card card) {
        paymentInstructionService.updatePaymentInstruction(id,card);
        return new ResponseEntity<>(HttpStatus.OK);
    }



    @ApiOperation(value = "Create cheque payment instruction", notes = "Create cheque payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Cheque payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cheques")
    public PaymentInstruction saveChequeInstruction(
        @Valid @RequestBody Cheque cheque) {
        ChequePaymentInstruction chequePaymentInstruction = ChequePaymentInstruction.chequePaymentInstructionWith()
            .payerName(cheque.getPayerName())
            .amount(cheque.getAmount())
            .currency(cheque.getCurrency())
            .status(cheque.getStatus())
            .chequeNumber(cheque.getChequeNumber()).build();
        return paymentInstructionService.createPaymentInstruction(chequePaymentInstruction);
    }

    @ApiOperation(value = "Update cheque payment instruction", notes = "Update cheque payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Postal Order payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/cheques/{id}")
    public ResponseEntity<Void> updateChequeInstruction(@PathVariable("id") Integer id , @ApiParam(value="Cheque request",required=true) @Valid @RequestBody Cheque cheque) {
        paymentInstructionService.updatePaymentInstruction(id,cheque);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @ApiOperation(value = "Reject the payment instruction", notes = "Reject payment instruction with the given id.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Payment instruction rejected"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/payment-instructions/{id}/reject")
	public ResponseEntity<Void> rejectPaymentInstruction(@PathVariable("id") Integer id) {
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
		paymentInstructionService.updatePaymentInstruction(id, paymentInstructionRequest);
		return new ResponseEntity<>(HttpStatus.OK);
	}


    @ApiOperation(value = "Create cash payment instruction", notes = "Create cash payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Cash payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/cash")
    public PaymentInstruction saveCashInstruction(@ApiParam(value="Cash request",required=true) @Valid @RequestBody Cash cash) {
        CashPaymentInstruction cashPaymentInstruction = CashPaymentInstruction.cashPaymentInstructionWith()
            .payerName(cash.getPayerName())
            .amount(cash.getAmount())
            .status(cash.getStatus())
            .currency(cash.getCurrency()).build();
        return paymentInstructionService.createPaymentInstruction(cashPaymentInstruction);
    }


    @ApiOperation(value = "Update cash payment instruction", notes = "Update cash payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Cash payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/cash/{id}")
    public ResponseEntity<Void> updateCashInstruction(@PathVariable("id") Integer id , @ApiParam(value="Cash request",required=true) @Valid @RequestBody Cash cash) {
        paymentInstructionService.updatePaymentInstruction(id,cash);
        return new ResponseEntity<>(HttpStatus.OK);
    }




    @ApiOperation(value = "Create poatal order payment instruction", notes = "Create postal order payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Postal order payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/postal-orders")
    public PaymentInstruction savePostalOrderInstruction(
        @ApiParam(value="Postal Order request",required=true) @Valid @RequestBody PostalOrder postalOrder) {
        PostalOrderPaymentInstruction postalOrderPaymentInstruction = PostalOrderPaymentInstruction.postalOrderPaymentInstructionWith()
            .payerName(postalOrder.getPayerName())
            .amount(postalOrder.getAmount())
            .currency(postalOrder.getCurrency())
            .status(postalOrder.getStatus())
            .postalOrderNumber(postalOrder.getPostalOrderNumber()).build();
        return paymentInstructionService.createPaymentInstruction(postalOrderPaymentInstruction);
    }

    @ApiOperation(value = "Update postal order payment instruction", notes = "Update postal order payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Postal Order payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/postal-orders/{id}")
    public ResponseEntity<Void> updatePostalOrderInstruction(@PathVariable("id") Integer id , @ApiParam(value="Postal order request",required=true) @Valid @RequestBody PostalOrder postalOrder) {
        paymentInstructionService.updatePaymentInstruction(id,postalOrder);
        return new ResponseEntity<>(HttpStatus.OK);
    }



    @ApiOperation(value = "Create allpay payment instruction", notes = "Create allpay payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "AllPay payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/allpay")
    public PaymentInstruction saveAllPayInstruction(
        @ApiParam(value="All Pay request", required=true) @Valid @RequestBody AllPay allPay) {
        AllPayPaymentInstruction allPayPaymentInstruction = AllPayPaymentInstruction.allPayPaymentInstructionWith()
            .payerName(allPay.getPayerName())
            .amount(allPay.getAmount())
            .currency(allPay.getCurrency())
            .status(allPay.getStatus())
            .allPayTransactionId(allPay.getAllPayTransactionId()).build();
        return paymentInstructionService.createPaymentInstruction(allPayPaymentInstruction);
    }

    @ApiOperation(value = "Update allpay payment instruction", notes = "Update allpay payment instruction with the given values.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Allpay payment instruction updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/allpay/{id}")
    public ResponseEntity<Void> updateAllPayInstruction(@PathVariable("id") Integer id , @ApiParam(value="Allpay request",required=true) @Valid @RequestBody AllPay allpay) {
        paymentInstructionService.updatePaymentInstruction(id,allpay);
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
    public ResponseEntity<PaymentInstruction> submitPaymentInstructionsByPostClerk(@PathVariable("id") Integer id,
                                                                                   @RequestBody PaymentInstructionUpdateRequest paymentInstructionUpdateRequest) {
        if (null == paymentInstructionUpdateRequest) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        PaymentInstruction submittedPaymentInstruction = paymentInstructionService.submitPaymentInstruction(id, paymentInstructionUpdateRequest);
        return new ResponseEntity<>(submittedPaymentInstruction, HttpStatus.OK);
    }

    @ApiOperation(value = "Create case fee detail for a payment instruction", notes = "Create case fee detail for a payment instruction.")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Case fee detail for a payment instruction created"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/fees")
    public CaseFeeDetail saveCaseFeeDetail(@RequestBody CaseFeeDetailRequest caseFeeDetailRequest) {
        return caseFeeDetailService.saveCaseFeeDetail(caseFeeDetailRequest);
    }

    @ApiOperation(value = "Update case fee details", notes = "Update case fee details with the given values.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Case Fee details updated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error") })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/fees/{caseFeeId}")
    public CaseFeeDetail updateCaseFeeDetail(@PathVariable("caseFeeId") Integer caseFeeId,
                                             @RequestBody CaseFeeDetailRequest caseFeeDetailRequest) {
        return caseFeeDetailService.updateCaseFeeDetail(caseFeeId, caseFeeDetailRequest);
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
    @GetMapping("/payment-stats")
    @Deprecated
    public  MultiMap getPaymentStats(
        @RequestParam(name = "status", required = true) String status) {
    	Optional<BarUser> userOptional = barUserService.getBarUser();
		BarUser user = null; 
		if (userOptional.isPresent()) {
			user = userOptional.get();
		} else {
			return new MultiValueMap();
		}
		return paymentInstructionService.getPaymentInstructionStats(status);
    }
    
    @ApiOperation(value = "Get the payments stats", notes = "Get the payment instruction's stats showing each User's activities.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Return payment overview stats"),
        @ApiResponse(code = 500, message = "Internal server error") })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/pi-stats")
	public MultiMap getPIStats(@RequestParam(name = "status", required = true) PaymentStatusEnum status) {
		return paymentInstructionService.getPaymentInstructionStats(status.dbKey());
	}
    
	@ApiOperation(value = "Get the rejected payments stats", notes = "Get the payment instruction's rejected stats showing each User's activities.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Return rejected payment stats"),
		@ApiResponse(code = 500, message = "Internal server error") })
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/users/pi-rejected-stats")
	public MultiMap getPIRejectedStats(
			@RequestParam(name = "currentStatus", required = true) PaymentStatusEnum currentStatus,
			@RequestParam(name = "oldStatus", required = true) PaymentStatusEnum oldStatus) {
		return paymentInstructionService
				.getPaymentInstructionStatsByCurrentStatusGroupedByOldStatus(currentStatus.dbKey(), oldStatus.dbKey());
	}

    private PaymentInstructionSearchCriteriaDto createPaymentInstructionCriteria(
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String payerName,
        String chequeNumber,
        String postalOrderNumber,
        Integer dailySequenceId,
        String allPayInstructionId,
        String paymentType,
        String action,
        String caseReference
    ){
        return createPaymentInstructionCriteria(null, status, startDate, endDate, payerName, chequeNumber,
            postalOrderNumber, dailySequenceId, allPayInstructionId, paymentType, action, caseReference);
    }

    private PaymentInstructionSearchCriteriaDto createPaymentInstructionCriteria(
        String userId,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String payerName,
        String chequeNumber,
        String postalOrderNumber,
        Integer dailySequenceId,
        String allPayInstructionId,
        String paymentType,
        String action,
        String caseReference
    ){
        return PaymentInstructionSearchCriteriaDto
            .paymentInstructionSearchCriteriaDto().status(status)
            .userId(userId)
            .startDate(startDate == null ? null : startDate.atStartOfDay())
            .endDate(endDate == null ? null : endDate.atTime(LocalTime.now())).payerName(payerName)
            .chequeNumber(chequeNumber).postalOrderNumer(postalOrderNumber).dailySequenceId(dailySequenceId)
            .allPayInstructionId(allPayInstructionId).paymentType(paymentType).action(action)
            .caseReference(caseReference).build();
    }

    private boolean checkAcceptHeaderForCsv(HttpHeaders headers){
        if (headers.getAccept().contains(new MediaType("text","csv"))){
            return true;
        }
        return false;
    }
    
    @InitBinder
	public void initBinder(final WebDataBinder webdataBinder) {
		webdataBinder.registerCustomEditor(PaymentStatusEnum.class, new PaymentStatusEnumConverter());
	}

}
