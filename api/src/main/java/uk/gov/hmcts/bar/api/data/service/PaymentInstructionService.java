package uk.gov.hmcts.bar.api.data.service;


import com.google.common.collect.Lists;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.ff4j.FF4j;
import org.ff4j.exception.FeatureAccessException;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.bar.api.audit.AuditRepository;
import uk.gov.hmcts.bar.api.controllers.payment.PaymentInstructionController;
import uk.gov.hmcts.bar.api.data.enums.BarUserRoleEnum;
import uk.gov.hmcts.bar.api.data.enums.PaymentActionEnum;
import uk.gov.hmcts.bar.api.data.enums.PaymentStatusEnum;
import uk.gov.hmcts.bar.api.data.exceptions.PaymentInstructionNotFoundException;
import uk.gov.hmcts.bar.api.data.exceptions.PaymentProcessException;
import uk.gov.hmcts.bar.api.data.model.*;
import uk.gov.hmcts.bar.api.data.repository.*;
import uk.gov.hmcts.bar.api.data.utils.Util;
import uk.gov.hmcts.bar.api.integration.payhub.data.PayhubFullRemission;
import uk.gov.hmcts.bar.api.integration.payhub.data.PayhubPaymentInstruction;
import uk.gov.hmcts.bar.api.integration.payhub.repository.PayhubFullRemissionRepository;
import uk.gov.hmcts.bar.api.integration.payhub.repository.PayhubPaymentInstructionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


@Service
@Transactional
public class PaymentInstructionService {

    private static final String[] ALWAYS_UPDATE = new String[]{ "actionComment", "actionReason" };

    public static final String STAT_GROUP_DETAILS = "stat-group-details";
    public static final String STAT_DETAILS = "stat-details";

    private static final Logger LOG = getLogger(PaymentInstructionService.class);

    private static final List<String> GROUPED_TYPES = Arrays.asList("CHEQUE", "POSTAL_ORDER");

    private static final int PAGE_NUMBER = 0;
    private static final int MAX_RECORDS_PER_PAGE = 200;
    private PaymentInstructionRepository paymentInstructionRepository;
    private PaymentInstructionStatusRepository paymentInstructionStatusRepository;
    private PaymentReferenceService paymentReferenceService;
    private final BankGiroCreditRepository bankGiroCreditRepository;
    private final FF4j ff4j;
    private PaymentTypeService paymentTypeService;
    private final PayhubPaymentInstructionRepository payhubPaymentInstructionRepository;
    private final PayhubFullRemissionRepository payhubFullRemissionRepository;
    private final AuditRepository auditRepository;
    private final PaymentInstructionUpdateValidatorService updateValidatorService;


    public PaymentInstructionService(PaymentReferenceService paymentReferenceService, PaymentInstructionRepository paymentInstructionRepository,
                                     PaymentInstructionStatusRepository paymentInstructionStatusRepository,
                                     FF4j ff4j,
                                     BankGiroCreditRepository bankGiroCreditRepository,
                                     PaymentTypeService paymentTypeService,
                                     PaymentInstructionUpdateValidatorService updateValidatorService,
                                     PayhubPaymentInstructionRepository payhubPaymentInstructionRepository,
                                     PayhubFullRemissionRepository payhubFullRemissionRepository,
                                     AuditRepository auditRepository

    ) {
        this.paymentReferenceService = paymentReferenceService;
        this.paymentInstructionRepository = paymentInstructionRepository;
        this.paymentInstructionStatusRepository = paymentInstructionStatusRepository;
        this.ff4j = ff4j;
        this.bankGiroCreditRepository = bankGiroCreditRepository;
        this.paymentTypeService = paymentTypeService;
        this.payhubPaymentInstructionRepository = payhubPaymentInstructionRepository;
        this.auditRepository = auditRepository;
        this.updateValidatorService = updateValidatorService;
        this.payhubFullRemissionRepository = payhubFullRemissionRepository;
    }

    public PaymentInstruction createPaymentInstruction(BarUser barUser, PaymentInstruction paymentInstruction)  {
        PaymentReference nextPaymentReference = paymentReferenceService.getNextPaymentReference(barUser.getSelectedSiteId());
        paymentInstruction.setSiteId(barUser.getSelectedSiteId());
        paymentInstruction.setDailySequenceId(getDailySequentialPaymentId(nextPaymentReference));
        paymentInstruction.setStatus(PaymentStatusEnum.DRAFT.dbKey());
        paymentInstruction.setUserId(barUser.getId());
        PaymentInstruction savedPaymentInstruction = paymentInstructionRepository.saveAndRefresh(paymentInstruction);
        savePaymentInstructionStatus(savedPaymentInstruction, barUser.getId());
        if ((barUser.getRoles().contains(BarUserRoleEnum.BAR_FEE_CLERK.getIdamRole()))) {
            savedPaymentInstruction.setStatus(PaymentStatusEnum.PENDING.dbKey());
            paymentInstructionRepository.saveAndRefresh(savedPaymentInstruction);
            savePaymentInstructionStatus(savedPaymentInstruction,barUser.getId());
        }
        auditRepository.trackPaymentInstructionEvent("CREATE_PAYMENT_INSTRUCTION_EVENT", paymentInstruction, barUser);
        return savedPaymentInstruction;
    }

    public List<PaymentInstruction> getAllPaymentInstructions(BarUser barUser,  PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto)  {
        paymentInstructionSearchCriteriaDto.setSiteId(barUser.getSelectedSiteId());
        PaymentInstructionsSpecifications<PaymentInstruction> paymentInstructionsSpecification = new PaymentInstructionsSpecifications<>(paymentInstructionSearchCriteriaDto,paymentTypeService);
        Sort sort = Sort.by(Sort.Direction.DESC, "paymentDate");
        Pageable pageDetails = PageRequest.of(PAGE_NUMBER, MAX_RECORDS_PER_PAGE, sort);

        Specification<PaymentInstruction> piSpecification = null;
        if (paymentInstructionSearchCriteriaDto.getMultiplePiIds() != null) {
            piSpecification = paymentInstructionsSpecification.getPaymentInstructionsMultipleIdSpecification();
        } else {
            piSpecification = paymentInstructionsSpecification.getPaymentInstructionsSpecification();
        }

        return Lists.newArrayList(paymentInstructionRepository.findAll(piSpecification, pageDetails).iterator());
    }

    public long getPaymentInstructionsCount(PaymentInstructionStatusCriteriaDto paymentInstructionStatusCriteriaDto) {
        PaymentInstructionStatusSpecifications<PaymentInstructionStatus> paymentInstructionStatusSpecification = new PaymentInstructionStatusSpecifications(paymentInstructionStatusCriteriaDto);
        Specification<PaymentInstructionStatus>  pisSpecification = paymentInstructionStatusSpecification.getPaymentInstructionStatusSpecification();
        return paymentInstructionStatusRepository.count(pisSpecification);

    }


    public long getNonResetPaymentInstructionsCount(String status, String siteId) {
        return paymentInstructionStatusRepository.getNonResetCountByStatus(status, siteId);
    }

    public List<PayhubPaymentInstruction> getAllPaymentInstructionsForPayhub(
        BarUser barUser,
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto
    )  {
        paymentInstructionSearchCriteriaDto.setSiteId(barUser.getSelectedSiteId());
        PaymentInstructionsSpecifications<PayhubPaymentInstruction> paymentInstructionsSpecification =
            new PaymentInstructionsSpecifications<>(paymentInstructionSearchCriteriaDto, paymentTypeService);

        Specification<PayhubPaymentInstruction> piForPayhubSpecification = paymentInstructionsSpecification.getPaymentInstructionsSpecification();
        return payhubPaymentInstructionRepository.findAll(piForPayhubSpecification);
    }

    public List<PayhubFullRemission> getAllRemissionsForPayhub(BarUser barUser,
                                                                    PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto)  {
        paymentInstructionSearchCriteriaDto.setSiteId(barUser.getSelectedSiteId());
        PaymentInstructionsSpecifications<PayhubFullRemission> paymentInstructionsSpecification =
            new PaymentInstructionsSpecifications<>(paymentInstructionSearchCriteriaDto, paymentTypeService);

        Specification<PayhubFullRemission> piForPayhubSpecification = paymentInstructionsSpecification.getPaymentInstructionsSpecification();
        return payhubFullRemissionRepository.findAll(piForPayhubSpecification);
    }

    public PaymentInstruction getPaymentInstruction(Integer id, String siteId) {
        Optional<PaymentInstruction> op = paymentInstructionRepository.findByIdAndSiteId(id, siteId);
        return op.orElse(null);
    }

    public void deletePaymentInstruction(Integer id, String siteId) {
        paymentInstructionStatusRepository.deleteByPaymentInstructionId(id, siteId);
        int deletedPayment = paymentInstructionRepository.deleteByIdAndSiteId(id, siteId);
        if (deletedPayment <= 0){
            throw new PaymentInstructionNotFoundException(id);
        }
    }

    public PaymentInstruction submitPaymentInstruction(BarUser barUser, Integer id, PaymentInstructionUpdateRequest paymentInstructionUpdateRequest) throws PaymentProcessException {
        if (!checkIfActionEnabled(paymentInstructionUpdateRequest)) {
            throw new FeatureAccessException(paymentInstructionUpdateRequest.getAction() + " is not allowed");
        }
        Optional<PaymentInstruction> optionalPaymentInstruction = paymentInstructionRepository.findByIdAndSiteId(id, barUser.getSelectedSiteId());
        PaymentInstruction existingPaymentInstruction = optionalPaymentInstruction
            .orElseThrow(() -> new PaymentInstructionNotFoundException(id));

        updateValidatorService.validateAll(existingPaymentInstruction, paymentInstructionUpdateRequest);

        // Assign post clerk payments to fee clerk
        if (paymentInstructionUpdateRequest.getStatus().equals(PaymentStatusEnum.DRAFT.dbKey())){
            paymentInstructionUpdateRequest.setStatus(PaymentStatusEnum.PENDING.dbKey());
        }

        updatePaymentInstructionsProps(existingPaymentInstruction, paymentInstructionUpdateRequest);
		if (PaymentStatusEnum.PENDING.dbKey().equals(paymentInstructionUpdateRequest.getStatus())) {
			existingPaymentInstruction.setAction(null);
			existingPaymentInstruction.setActionReason(null);
			existingPaymentInstruction.setActionComment(null);
		}
        existingPaymentInstruction.setUserId(barUser.getId());
        savePaymentInstructionStatus(existingPaymentInstruction, barUser.getId());
        PaymentInstruction paymentInstruction = paymentInstructionRepository.saveAndRefresh(existingPaymentInstruction);

        auditRepository.trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",existingPaymentInstruction, barUser);

        return paymentInstruction;
    }

    public PaymentInstruction updatePaymentInstruction(BarUser barUser, Integer id, PaymentInstructionRequest paymentInstructionRequest)  {
        Optional<PaymentInstruction> optionalPaymentInstruction = paymentInstructionRepository.findByIdAndSiteId(id, barUser.getSelectedSiteId());
        PaymentInstruction existingPaymentInstruction = optionalPaymentInstruction
            .orElseThrow(() -> new PaymentInstructionNotFoundException(id, barUser.getSelectedSiteId()));

        // handle bgc number
        if (paymentInstructionRequest.getBgcNumber() != null) {
            BankGiroCredit bgc = bankGiroCreditRepository.findByBgcNumber(paymentInstructionRequest.getBgcNumber())
                .orElseGet(() -> bankGiroCreditRepository.save(new BankGiroCredit(paymentInstructionRequest.getBgcNumber(), barUser.getSelectedSiteId())));
            existingPaymentInstruction.setBgcNumber(bgc.getBgcNumber());
        }

        updatePaymentInstructionsProps(existingPaymentInstruction, paymentInstructionRequest);
        existingPaymentInstruction.setUserId(barUser.getId());
        savePaymentInstructionStatus(existingPaymentInstruction, barUser.getId());
        PaymentInstruction paymentInstruction = paymentInstructionRepository.saveAndRefresh(existingPaymentInstruction);
        auditRepository.trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",existingPaymentInstruction,barUser);
        return paymentInstruction;
    }

    public List<PaymentInstruction> getAllPaymentInstructionsByCaseReference(String caseReference) {
        return paymentInstructionRepository.findByCaseReference(caseReference);
    }


    public MultiMap getPaymentInstructionStats(String status,boolean sentToPayhub, String siteId) {
        List<PaymentInstructionUserStats> paymentInstructionInStatusList = paymentInstructionStatusRepository
            .getPaymentInstructionsByStatusGroupedByUser(status,sentToPayhub, siteId);

        return Util.createMultimapFromList(paymentInstructionInStatusList);
    }
    public MultiMap getPaymentInstructionStatsWithCount(String status, String siteId, LocalDateTime startDate, LocalDateTime endDate) {
        List<PaymentInstructionUserStatsWithRole> paymentInstructionInStatusWithRoleList = paymentInstructionStatusRepository
            .getPaymentInstructionsByStatusGroupedByUserWithCount(status, siteId,startDate,endDate);

        return Util.createMultimapFromListWithRole(paymentInstructionInStatusWithRoleList);
    }
    public MultiMap getPaymentInstructionStatsByCurrentStatusGroupedByOldStatus(String currentStatus,
                                                                                String oldStatus,
                                                                                String siteId) {
        List<PaymentInstructionStaticsByUser> paymentInstructionStaticsByUserObjects = paymentInstructionStatusRepository
            .getPaymentInstructionStatsByCurrentStatusAndByOldStatus(currentStatus, oldStatus, siteId);
        paymentInstructionStaticsByUserObjects = Util.getFilteredPisList(paymentInstructionStaticsByUserObjects);
        return Util.createMultimapFromPisByUserList(paymentInstructionStaticsByUserObjects);
    }

    public MultiMap getPaymentStatsByUserGroupByType(String userId, String status, Optional<String> oldStatus, boolean sentToPayhub, String siteId) {
        String oldPaymentStatus = oldStatus.orElse(status);
        List<PaymentInstructionStats> results = paymentInstructionStatusRepository.getStatsByUserGroupByType(userId, status, oldPaymentStatus, sentToPayhub, siteId);

        return createHateoasResponse(results, userId, status, oldStatus.orElse(null));
    }

    public MultiMap getPaymentInstructionsByUserGroupByActionAndType(String userId, String status, Optional<String> oldStatus, boolean sentToPayhub, String siteId) {
        String oldPaymentStatus = oldStatus.orElse(status);
        List<PaymentInstructionStats> results =  paymentInstructionStatusRepository.getStatsByUserGroupByActionAndType(userId, status, oldPaymentStatus, sentToPayhub, siteId);

        return createHateoasResponse(results, userId, status, oldStatus.orElse(null));
    }

    private MultiMap createHateoasResponse(List<PaymentInstructionStats> stats, String userId, String status, String oldStatus) {
        MultiMap paymentInstructionStatsGroupedByBgc = new MultiValueMap();
        stats.stream().forEach(stat -> {
            String bgcNumber = stat.getBgc() == null ? PaymentInstructionsSpecifications.IS_NULL : stat.getBgc();
            Link detailslink = createHateoasLink(userId, status, stat.getPaymentType(), stat.getAction(), bgcNumber, STAT_DETAILS, oldStatus);

            EntityModel<PaymentInstructionStats> resource = new EntityModel<>(stat, detailslink.expand());

            // TODO: this is just a temp solution we have to clarify with PO if we really need to group cheques and postal-orders
            if (GROUPED_TYPES.contains(stat.getPaymentType())) {
                String paymentTypes = GROUPED_TYPES.stream().collect(Collectors.joining(","));
                Link groupedLink = createHateoasLink(userId, status, paymentTypes, stat.getAction(), bgcNumber, STAT_GROUP_DETAILS, oldStatus);
                resource.add(groupedLink.expand());
            }

            paymentInstructionStatsGroupedByBgc.put(stat.getBgc() == null || stat.getBgc().equals("")  ? "0" : stat.getBgc(), resource);
        });
        return paymentInstructionStatsGroupedByBgc;
    }

    private Link createHateoasLink(String userId, String status, String paymentType, String action, String bgcNumber,
                                   String rel, String oldStatus) {

        return linkTo(methodOn(PaymentInstructionController.class)
            .getPaymentInstructionsByIdamId(null, userId, status,
                null, null, null, null, null,
                null, null, null, paymentType, action, null, bgcNumber, oldStatus,null)
        ).withRel(rel);
    }

    public void savePaymentInstructionStatus(PaymentInstruction pi, String userId) {
        PaymentInstructionStatus pis = new PaymentInstructionStatus(userId, pi);
        paymentInstructionStatusRepository.save(pis);
    }

    public Map<Integer, List<PaymentInstructionStatusHistory>> getStatusHistoryMapForTTB(LocalDate startDate,  LocalDate endDate, String siteId) {

        if (null != endDate && startDate.isAfter(endDate)) {
            LOG.error("PaymentInstructionService - Error while generating daily fees csv file. Incorrect start and end dates ");
            return Collections.emptyMap();
        }
        LocalDate searchEndDate;

        if (null == endDate || startDate.equals(endDate)) {
            searchEndDate = startDate.plusDays(1);
        } else {
            searchEndDate = endDate.plusDays(1);
        }

        List<PaymentInstructionStatusHistory> statusHistoryList = paymentInstructionStatusRepository.getPaymentInstructionStatusHistoryForTTB
            (startDate.atStartOfDay(), searchEndDate.atStartOfDay(),siteId);

        final Map<Integer, List<PaymentInstructionStatusHistory>> statusHistoryMapByPaymentInstructionId = new HashMap<>();
        for (final PaymentInstructionStatusHistory statusHistory : statusHistoryList) {
            if (statusHistoryMapByPaymentInstructionId.get(statusHistory.getPaymentInstructionId()) == null) {
                List<PaymentInstructionStatusHistory> listByPaymentInstructionId = new ArrayList<>();
                listByPaymentInstructionId.add(statusHistory);
                statusHistoryMapByPaymentInstructionId.put(statusHistory.getPaymentInstructionId(), listByPaymentInstructionId);
            } else {
                statusHistoryMapByPaymentInstructionId.get(statusHistory.getPaymentInstructionId()).add(statusHistory);
            }

        }
        return statusHistoryMapByPaymentInstructionId;
    }

    public List<PaymentInstruction> getAllPaymentInstructionsByTTB(LocalDate startDate, LocalDate endDate, String siteId) {
        Map<Integer, List<PaymentInstructionStatusHistory>> statusHistortMapForTTB = getStatusHistoryMapForTTB(startDate, endDate,siteId);
        Iterator<Map.Entry<Integer, List<PaymentInstructionStatusHistory>>> iterator = statusHistortMapForTTB.entrySet().iterator();
        List<PaymentInstruction> paymentInstructionsList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<Integer, List<PaymentInstructionStatusHistory>> entry = iterator.next();
            PaymentInstruction paymentInstruction = paymentInstructionRepository.getOne(entry.getKey());
            paymentInstruction.setPaymentInstructionStatusHistory(entry.getValue());
            paymentInstructionsList.add(paymentInstruction);

        }
        return paymentInstructionsList;
    }

    private boolean checkIfActionEnabled(PaymentInstructionUpdateRequest paymentInstructionUpdateRequest){
        boolean[] ret = { true };
        String action = paymentInstructionUpdateRequest.getAction();
        PaymentActionEnum.findByDisplayValue(action).ifPresent(paymentActionEnum ->
            ret[0] = ff4j.check(paymentActionEnum.featureKey()));
        return ret[0];
    }

    private void updatePaymentInstructionsProps(PaymentInstruction existingPi, Object updateRequest) {
        String[] nullPropertiesNamesToIgnore = Util.getNullPropertyNames(updateRequest);
        String[] propNamesToIgnore = Arrays.stream(nullPropertiesNamesToIgnore)
            .filter(s -> Arrays.stream(ALWAYS_UPDATE).noneMatch(s::equals))
            .toArray(String[]::new);
        BeanUtils.copyProperties(updateRequest, existingPi, propNamesToIgnore);
    }

    private String getDailySequentialPaymentId(PaymentReference paymentReference){

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02d", LocalDate.now().getDayOfMonth()))
            .append(paymentReference.getSequenceCharacter())
            .append(String.format("%04d",paymentReference.getSequenceId()));

        return sb.toString();
    }

}
