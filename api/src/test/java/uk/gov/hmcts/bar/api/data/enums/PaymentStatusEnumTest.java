package uk.gov.hmcts.bar.api.data.enums;

import org.junit.Assert;
import org.junit.Test;

public class PaymentStatusEnumTest {


    @Test
    public void testFindPaymentStatusByDbKey() {
        PaymentStatusEnum statusEnum = PaymentStatusEnum.getPaymentStatusEnum("D");
        Assert.assertEquals(PaymentStatusEnum.DRAFT, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("P");
        Assert.assertEquals(PaymentStatusEnum.PENDING, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("V");
        Assert.assertEquals(PaymentStatusEnum.VALIDATED, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("PA");
        Assert.assertEquals(PaymentStatusEnum.PENDING_REVIEW, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("A");
        Assert.assertEquals(PaymentStatusEnum.REVIEWED, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("TTB");
        Assert.assertEquals(PaymentStatusEnum.TRANSFERREDTOBAR, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("C");
        Assert.assertEquals(PaymentStatusEnum.COMPLETED, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("REJ");
        Assert.assertEquals(PaymentStatusEnum.REJECTED, statusEnum);

        statusEnum = PaymentStatusEnum.getPaymentStatusEnum("RDM");
        Assert.assertEquals(PaymentStatusEnum.REJECTEDBYDM, statusEnum);


    }


}
