package uk.gov.hmcts.bar.api.data.utils;

import uk.gov.hmcts.bar.api.data.enums.PaymentStatusEnum;

import java.beans.PropertyEditorSupport;

public class PaymentStatusEnumConverter extends PropertyEditorSupport {
	@Override
    public void setAsText(final String text) {
        String modifiedText;
        if (text.equals("PA"))
            modifiedText = "PR";
        else if (text.equals("A"))
            modifiedText = "R";
        else
            modifiedText = text;
        setValue(PaymentStatusEnum.getPaymentStatusEnum(modifiedText));
    }
}
