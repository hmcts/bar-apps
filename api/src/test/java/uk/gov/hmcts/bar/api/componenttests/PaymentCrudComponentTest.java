package uk.gov.hmcts.bar.api.componenttests;

import org.junit.Test;
import uk.gov.hmcts.bar.api.contract.PaymentDto;
import uk.gov.hmcts.bar.api.contract.PaymentUpdateDto;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.bar.api.contract.PaymentUpdateDto.CaseUpdateDto.caseUpdateDtoWith;
import static uk.gov.hmcts.bar.api.contract.PaymentUpdateDto.paymentUpdateDtoWith;

public class PaymentCrudComponentTest extends ComponentTestBase {


    @Test
    public void create() throws Exception {
        PaymentUpdateDto.PaymentUpdateDtoBuilder proposedPayment = paymentUpdateDtoWith()
            .payeeName("Mr Payer Payer")
            .amount(500)
            .sortCode("000000")
            .chequeNumber("000000")
            .accountNumber("00000000")
            .paymentReceiptType("post")
            .paymentType(1)
            .feeCode("X0001")
            .eventType("someevent")
            .counterCode("C001")
            .currencyType("GBP")
            .paymentDate("2017-10-10T00:00:00")
            .updateDate("2017-10-10T00:00:00")
            .updatedByUserId("user01")
            .cases(Arrays.asList(
                caseUpdateDtoWith().jurisdiction1("one").jurisdiction2("two").reference("case1").subServiceId("1").build())
            )
            .createdByUserId("user01");

        restActions
            .withUser("admin")
            .post("/payments", proposedPayment.build())
            .andExpect(status().isOk())
            .andExpect(body().as(PaymentDto.class, paymentDto -> {
                assertThat(paymentDto.getPayeeName()).isEqualTo("Mr Payer Payer");
                assertThat(paymentDto.getAmount()).isEqualTo(500);
                assertThat(paymentDto.getSortCode()).isEqualTo("000000");
                assertThat(paymentDto.getChequeNumber()).isEqualTo("000000");
                assertThat(paymentDto.getAccountNumber()).isEqualTo("00000000");
                assertThat(paymentDto.getPaymentReceiptType()).isEqualTo("post");
                assertThat(paymentDto.getPaymentType()).isEqualTo(1);
                assertThat(paymentDto.getFeeCode()).isEqualTo("X0001");
                assertThat(paymentDto.getEventType()).isEqualTo("someevent");
                assertThat(paymentDto.getCounterCode()).isEqualTo("C001");
                assertThat(paymentDto.getCurrencyType()).isEqualTo("GBP");
                assertThat(paymentDto.getPaymentDate()).isEqualTo("2017-10-10T00:00");
                assertThat(paymentDto.getUpdateDate()).isEqualTo("2017-10-10T00:00");
                assertThat(paymentDto.getUpdatedByUserId()).isEqualTo("user01");
                assertThat(paymentDto.getCases().size() == 1);
                assertThat(paymentDto.getCreatedByUserId()).isEqualTo("user01");
            }));
    }


}


