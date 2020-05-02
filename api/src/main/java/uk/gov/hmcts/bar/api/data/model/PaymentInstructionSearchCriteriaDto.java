package uk.gov.hmcts.bar.api.data.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Builder(builderMethodName = "paymentInstructionSearchCriteriaDto")
public class PaymentInstructionSearchCriteriaDto {

	private String siteId;
    private String dailySequenceId;
    private String payerName;
    @Pattern(regexp ="^\\d{6,6}$",message = "invalid cheque number")
    private String chequeNumber;
    @Pattern(regexp = "^\\d{6,6}$", message = "invalid postal order number")
    private String postalOrderNumer;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Pattern(regexp ="^\\d{1,20}$",message = "invalid all pay transaction id")
    private String allPayInstructionId;
    private String paymentType;
    private String action;
    private String userId;
    private String caseReference;
    private String multiplePiIds;
    private String bgcNumber;
    private Boolean transferredToPayhub;
    private String authorizationCode;
    private String oldStatus;
    private String payhubReference;
    private String pageNumber;
    private String recordsPerPage;

}
