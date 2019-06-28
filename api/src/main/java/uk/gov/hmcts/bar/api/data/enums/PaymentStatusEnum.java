package uk.gov.hmcts.bar.api.data.enums;

import java.util.HashMap;
import java.util.Map;

public enum PaymentStatusEnum {

    DRAFT("D", "Draft"), PENDING("P", "Pending"), PENDING_APPROVAL("PA","Pending Approval"),APPROVED("A","Approved"),VALIDATED("V", "Validated"), PENDING_REVIEW("PR",
        "Pending Review"), REVIEWED("R", "Reviewed"), TRANSFERREDTOBAR("TTB",
        "Transferred to bar"), REJECTED("REJ", "Rejected"), REJECTEDBYDM("RDM", "Rejected by DM"), COMPLETED("C","Completed");
	private static final Map<String, PaymentStatusEnum> paymentStatusEnumMap = new HashMap<>();

	static {
		PaymentStatusEnum[] paymentStatusEnums = PaymentStatusEnum.values();
		for (PaymentStatusEnum paymentStatusEnum : paymentStatusEnums) {
			paymentStatusEnumMap.put(paymentStatusEnum.dbKey(), paymentStatusEnum);
		}
	}

	private String dbKey = null;

	private String displayValue = null;

	PaymentStatusEnum(String dbKey, String displayValue) {
		this.dbKey = dbKey;
		this.displayValue = displayValue;
	}

	public String dbKey() {
		return this.dbKey;
	}

	public String displayValue() {
		return this.displayValue;
	}

	public static PaymentStatusEnum getPaymentStatusEnum(String dbKey) {
		return paymentStatusEnumMap.get(dbKey);
	}

    public static boolean contains(String dbKey) {

        for (PaymentStatusEnum pse : PaymentStatusEnum.values()) {
            if (pse.dbKey().equals(dbKey)) {
                return true;
            }
        }

        return false;
    }

    /*
     * Overridden the default toString() method for the sake of Swagger
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
    	return this.dbKey();
    }

}
