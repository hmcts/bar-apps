package uk.gov.hmcts.bar.api.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.Pattern;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChequePaymentInstructionDto extends PaymentInstructionDto {

    @Pattern(regexp ="^\\d{6,6}$",message = "invalid sort code")
    private final String sortCode;

    @Pattern(regexp ="^\\d{8,8}$",message = "invalid account number")
    private final String accountNumber;

    @Pattern(regexp ="^\\d{6,6}$",message = "invalid cheque number")
    private final String chequeNumber;

    private static final String PAYMENT_INSTRUCTION_TYPE = "CHEQUE";

    @JsonCreator
    @Builder(builderMethodName = "chequePaymentInstructionDtoWith")
    public ChequePaymentInstructionDto(@JsonProperty("payer_name") String payerName,
                                       @JsonProperty("amount") Integer amount,
                                       @JsonProperty("currency") String currency,
                                       @JsonProperty("sort_code") String sortCode,
                                       @JsonProperty("account_number") String accountNumber,
                                       @JsonProperty("cheque_number") String chequeNumber) {
        super(payerName,amount,currency,PAYMENT_INSTRUCTION_TYPE);
        this.sortCode = sortCode;
        this.accountNumber = accountNumber ;
        this.chequeNumber = chequeNumber;
    }


}
