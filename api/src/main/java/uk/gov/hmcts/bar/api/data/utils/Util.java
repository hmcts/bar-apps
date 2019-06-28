package uk.gov.hmcts.bar.api.data.utils;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.ff4j.FF4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import uk.gov.hmcts.bar.api.data.enums.BarUserRoleEnum;
import uk.gov.hmcts.bar.api.data.enums.PaymentStatusEnum;
import uk.gov.hmcts.bar.api.data.model.PaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionStaticsByUser;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionUserStats;

import javax.persistence.criteria.CriteriaBuilder.In;
import java.beans.FeatureDescriptor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public interface Util {

    String CONVERT_STATUS_FOR_FRONTEND_FEATURE_KEY = "convert-status-for-frontend";

    static String[] getNullPropertyNames(Object source) {
        final BeanWrapper wrappedSource = new BeanWrapperImpl(source);
        return Stream.of(wrappedSource.getPropertyDescriptors())
            .map(FeatureDescriptor::getName)
            .filter(propertyName -> wrappedSource.getPropertyValue(propertyName) == null)
            .toArray(String[]::new);
    }

    static List<PaymentInstruction> updateStatusAndActionDisplayValue(
        final List<PaymentInstruction> paymentInstructions,FF4j ff4j) {

        return paymentInstructions.stream().map(paymentInstruction -> {
            String convertedStatus = null;
            if (isFeatureConvertStatusForFrontendEnabled(ff4j)) {
                convertedStatus = convertStatusForFrontend(paymentInstruction.getStatus());
            }
            else {
                convertedStatus = paymentInstruction.getStatus();
            }
            paymentInstruction
                .setStatus(PaymentStatusEnum.getPaymentStatusEnum(convertedStatus).displayValue());
            if (paymentInstruction.getAction() != null) {
                paymentInstruction.setAction(paymentInstruction.getAction());
            }
            return paymentInstruction;
        }).collect(Collectors.toList());

    }

    static In<String> getInCriteriaWithStringValues(In<String> inCriteria, String columnName) {
        if (inCriteria == null) {
            return null;
        }
        String[] valueArray = columnName.split(",");
        if (valueArray != null && valueArray.length > 0) {
            for (String statusValue : valueArray) {
                inCriteria.value(statusValue);
            }
        } else {
            inCriteria.value(columnName);
        }
        return inCriteria;
    }

	static In<Integer> getInCriteriaWithIntegerValues(In<Integer> inCriteria, String columnName) {
		if (inCriteria == null) {
			return null;
		}
		String[] valueArray = columnName.split(",");
		if (valueArray != null && valueArray.length > 0) {
			for (String statusValue : valueArray) {
				inCriteria.value(Integer.valueOf(statusValue));
			}
		} else {
			inCriteria.value(Integer.valueOf(columnName));
		}
		return inCriteria;
	}

	static List<PaymentInstructionStaticsByUser> getFilteredPisList(
			List<PaymentInstructionStaticsByUser> paymentInstructionStaticsByUserObjects) {
		Map<Integer, PaymentInstructionStaticsByUser> pisMap = new HashMap<>();
		paymentInstructionStaticsByUserObjects.forEach(pisByUser -> {
			Integer key = pisByUser.getPaymentInstructionId();
			if (pisMap.containsKey(key)) {
				PaymentInstructionStaticsByUser insertedObj = pisMap.get(key);
				if (insertedObj.getUpdateTime().isBefore(pisByUser.getUpdateTime())) {
					pisMap.put(key, pisByUser);
				}
			} else {
				pisMap.put(key, pisByUser);
			}
		});
		return new ArrayList<>(pisMap.values());
	}

    static String getFormattedDateTime(LocalDateTime localDateTime, DateTimeFormatter dateFormatter) {
        return (localDateTime == null || dateFormatter == null) ? null : localDateTime.format(dateFormatter);
    }

    static boolean isUserDeliveryManager(String userRoles) {
        return org.apache.commons.lang3.StringUtils.containsIgnoreCase(userRoles, BarUserRoleEnum.BAR_DELIVERY_MANAGER.getIdamRole());
    }

    static boolean isUserSrFeeClerk(String userRoles) {
        return org.apache.commons.lang3.StringUtils.containsIgnoreCase(userRoles, BarUserRoleEnum.BAR_SENIOR_CLERK.getIdamRole());
    }

    static MultiMap createMultimapFromList(List<PaymentInstructionUserStats> piStatsList) {
        MultiMap paymentInstructionStatsUserMap = new MultiValueMap();
        piStatsList.forEach(pius -> paymentInstructionStatsUserMap.put(pius.getBarUserId(), pius));
        return paymentInstructionStatsUserMap;
    }

	static MultiMap createMultimapFromPisByUserList(List<PaymentInstructionStaticsByUser> pisByUserList) {
		MultiMap paymentInstructionStatsByUserMapTemp = new MultiValueMap();
		MultiMap paymentInstructionStatsMap = new MultiValueMap();
		pisByUserList.forEach(piByUser -> paymentInstructionStatsByUserMapTemp.put(piByUser.getBarUserId(), piByUser));
		Set<String> keys = paymentInstructionStatsByUserMapTemp.keySet();
		for (String key : keys) {
			List<PaymentInstructionStaticsByUser> pisByUserListInner = (List<PaymentInstructionStaticsByUser>) paymentInstructionStatsByUserMapTemp
					.get(key);
			if (pisByUserList.size() > 0) {
				PaymentInstructionUserStats piuStats = new PaymentInstructionUserStats(
						pisByUserListInner.get(0).getBarUserId(), pisByUserListInner.get(0).getBarUserFullName(),
						(long) pisByUserListInner.size());
				Integer[] piArray = new Integer[pisByUserListInner.size()];
				int i = 0;
				for (PaymentInstructionStaticsByUser pisByUserObj : pisByUserListInner) {
					piArray[i++] = pisByUserObj.getPaymentInstructionId();
				}
				piuStats.setListOfPaymentInstructions(piArray);
				paymentInstructionStatsMap.put(key, piuStats);
			}
		}
		return paymentInstructionStatsMap;
	}

    static String convertStatusForBackend(String status){
        String convertedStatus = null;
        if(null != status) {
            if (status.equals("PA"))
                convertedStatus = "PR";
            else if (status.equals("A"))
                convertedStatus = "R";
            else
                convertedStatus = status;
        }
        return convertedStatus;

    }

    static Optional<String> convertOptionalStatusForBackend(Optional<String> status){
        String convertedStatus = null;

        if(status.isPresent()){
            convertedStatus = status.get();
            convertedStatus = convertStatusForBackend(convertedStatus);
            return Optional.of(convertedStatus);
        }

        return status;

    }


    static String convertStatusForFrontend(String status){
        String convertedStatus = null;
        if(null != status) {
            if (status.equals("PR"))
                convertedStatus = "PA";
            else if (status.equals("R"))
                convertedStatus = "A";
            else
                convertedStatus = status;
        }
        return convertedStatus;

    }

    static boolean isFeatureConvertStatusForFrontendEnabled(FF4j ff4j){
        boolean isEnabled = false;
        isEnabled = ff4j.check(CONVERT_STATUS_FOR_FRONTEND_FEATURE_KEY);
        return isEnabled;

    }


	interface StringUtils {
        static boolean isAnyBlank(java.lang.String...values) {
            for (java.lang.String value: values) {
                if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
                    return true;
                }
            }
            return false;
        }
    }

}
