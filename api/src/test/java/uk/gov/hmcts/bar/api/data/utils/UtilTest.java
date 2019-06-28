package uk.gov.hmcts.bar.api.data.utils;

import org.ff4j.FF4j;
import org.junit.Test;
import org.mockito.InjectMocks;
import uk.gov.hmcts.bar.api.data.model.AllPayPaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.PaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionUpdateRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class UtilTest {

    @InjectMocks
    private Util util;



    private FF4j ff4j = new FF4j().createFeature(Util.CONVERT_STATUS_FOR_FRONTEND_FEATURE_KEY).disable(Util.CONVERT_STATUS_FOR_FRONTEND_FEATURE_KEY);


	@Test
	public void whenPaymentInstructionWithNullPropertyValuesPassedIn_shouldReturnAllNullPropertyNames() {
		PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
				.status("D").action("S").build();
		List<String> nullPropertyNames = Arrays.asList(Util.getNullPropertyNames(pir));
		assertTrue(nullPropertyNames.size() == 2);
	}

	@Test
	public void whenPaymentIntructionListPassed_shouldReturnSameListWithCorrectStatusDisplayForDraft() {
	    List<PaymentInstruction> piListModified = null;
		List<PaymentInstruction> piList = new ArrayList<PaymentInstruction>();
		PaymentInstruction pi = new AllPayPaymentInstruction();
		pi.setStatus("D");
		piList.add(pi);
		piListModified = Util.updateStatusAndActionDisplayValue(piList,ff4j);
		assertTrue(piListModified.get(0).getStatus().equals("Draft"));
	}

	@Test
	public void whenPaymentIntructionListPassed_shouldReturnSameListWithCorrectStatusDisplayForPending() {
		List<PaymentInstruction> piListModified = null;
		List<PaymentInstruction> piList = new ArrayList<PaymentInstruction>();
		PaymentInstruction pi = new AllPayPaymentInstruction();
		pi.setStatus("P");
		piList.add(pi);
		piListModified = Util.updateStatusAndActionDisplayValue(piList,ff4j);
		assertTrue(piListModified.get(0).getStatus().equals("Pending"));
	}

	@Test
    public void givenLocalDateTimeAndFormatter_shouldReturnFormattedString(){

        LocalDateTime testDateTime = LocalDateTime.of(2018,05,23,00,00);
        String expectedDate = "23 May 2018";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        assertEquals(expectedDate,Util.getFormattedDateTime(testDateTime,formatter));

    }

    @Test
    public void givenLocalDateTimeAndFormatterWithSeconds_shouldReturnFormattedString(){

        LocalDateTime testDateTime = LocalDateTime.of(2018,05,23,14,8,06);
        String expectedDateTime = "23 May 2018 14:08:06";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        assertEquals(expectedDateTime,Util.getFormattedDateTime(testDateTime,formatter));

    }

    @Test
    public void givenDeliveryManagerRole_shouldReturnTrue() {
    	String userRole = "bar-delivery-manager";
    	assertTrue(Util.isUserDeliveryManager(userRole));
    }

    @Test
    public void givenSrFeeClerkRole_shouldReturnTrue() {
    	String userRole = "bar-senior-clerk";
    	assertTrue(Util.isUserSrFeeClerk(userRole));
    }

    @Test
    public void givenWrongDeliveryManagerRole_shouldReturnFalse() {
    	String userRole = "bar-senior-clerk";
    	assertFalse(Util.isUserDeliveryManager(userRole));
    }

    @Test
    public void givenWrongSrFeeClerkRole_shouldReturnFalse() {
    	String userRole = "bar-delivery-manager";
    	assertFalse(Util.isUserSrFeeClerk(userRole));
    }

}
