package uk.gov.hmcts.bar.api.data.service;

import com.google.common.collect.Lists;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.ff4j.FF4j;
import org.ff4j.exception.FeatureAccessException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.hateoas.Resource;
import uk.gov.hmcts.bar.api.audit.AuditRepository;
import uk.gov.hmcts.bar.api.data.TestUtils;
import uk.gov.hmcts.bar.api.data.enums.BarUserRoleEnum;
import uk.gov.hmcts.bar.api.data.enums.PaymentActionEnum;
import uk.gov.hmcts.bar.api.data.exceptions.BadRequestException;
import uk.gov.hmcts.bar.api.data.exceptions.MissingSiteIdException;
import uk.gov.hmcts.bar.api.data.exceptions.PaymentInstructionNotFoundException;
import uk.gov.hmcts.bar.api.data.exceptions.PaymentProcessException;
import uk.gov.hmcts.bar.api.data.model.*;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionSearchCriteriaDto.PaymentInstructionSearchCriteriaDtoBuilder;
import uk.gov.hmcts.bar.api.data.repository.BankGiroCreditRepository;
import uk.gov.hmcts.bar.api.data.repository.PaymentInstructionRepository;
import uk.gov.hmcts.bar.api.data.repository.PaymentInstructionStatusRepository;
import uk.gov.hmcts.bar.api.data.validators.ActionValidator;
import uk.gov.hmcts.bar.api.data.validators.FullRemissionValidator;
import uk.gov.hmcts.bar.api.data.validators.UnallocatedAmountValidator;
import uk.gov.hmcts.bar.api.integration.payhub.data.PayhubPaymentInstruction;
import uk.gov.hmcts.bar.api.integration.payhub.repository.PayhubFullRemissionRepository;
import uk.gov.hmcts.bar.api.integration.payhub.repository.PayhubPaymentInstructionRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.bar.api.data.service.PaymentInstructionService.STAT_DETAILS;
import static uk.gov.hmcts.bar.api.data.service.PaymentInstructionService.STAT_GROUP_DETAILS;

public class PaymentInstructionServiceTest {

    @InjectMocks
    private PaymentInstructionService paymentInstructionServiceMock;

    @Mock
    private PaymentInstructionRepository paymentInstructionRepository;

    @Mock
    private PayhubPaymentInstructionRepository payhubPaymentInstructionRepository;

    @Mock
    private PayhubFullRemissionRepository payhubFullRemissionRepository;

    @Mock
    private PaymentReferenceService paymentReferenceService;

    @Mock
    private PaymentReference paymentReferenceMock;

    @Mock
    private Page<PaymentInstruction> piPageMock;

    @Mock
    private Iterator<PaymentInstruction> piIteratorMock;

    @Mock
    private Page<PayhubPaymentInstruction> payhubPiPageMock;

    @Mock
    private Iterator<PayhubPaymentInstruction> payhubPiIteratorMock;

    @Mock
    private PaymentInstruction paymentInstructionMock;

    @Mock
    private ChequePaymentInstruction chequePaymentInstructionMock;

    @Mock
    private AllPayPaymentInstruction allpayPaymentInstructionMock;

    @Mock
    private PostalOrderPaymentInstruction postalOrderPaymentInstructionMock;

    @Mock
    private CashPaymentInstruction cashPaymentInstructionMock;

    @Mock
    private PaymentInstructionRequest paymentRequestMock;

    @Mock
    private BarUser barUserMock;

    @Mock
    private FF4j ff4jMock;

    @Mock
    private BankGiroCreditRepository bankGiroCreditRepositoryMock;

    @Mock
    private PaymentInstructionStatusRepository paymentInstructionStatusRepositoryMock;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private List<CaseFeeDetail> cfdList;

    private PaymentInstructionUpdateValidatorService validatorService;

    private PaymentInstructionStatus paymentInstructionStatus;

    private PaymentInstructionStatusReferenceKey paymentInstructionStatusReferenceKey;

    private PaymentInstructionService paymentInstructionService;

    private PaymentInstructionSearchCriteriaDtoBuilder paymentInstructionSearchCriteriaDtoBuilder;

    private PaymentInstructionStatusCriteriaDto.PaymentInstructionStatusCriteriaDtoBuilder paymentInstructionStatusCriteriaDtoBuilder;

    private PaymentTypeService paymentTypeService;
    private static final String BAR_POST_CLERK_ROLE = BarUserRoleEnum.BAR_POST_CLERK.getIdamRole();
    private static final String BAR_FEE_CLERK_ROLE = BarUserRoleEnum.BAR_FEE_CLERK.getIdamRole();


    @Mock
    private UnallocatedAmountService unallocatedAmountService;

    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
        unallocatedAmountService = new UnallocatedAmountService(paymentInstructionRepository);
        setUpValidator();
        paymentInstructionService = new PaymentInstructionService(
            paymentReferenceService,
            paymentInstructionRepository,
            paymentInstructionStatusRepositoryMock,
            ff4jMock,
            bankGiroCreditRepositoryMock,
            paymentTypeService,
            validatorService,
            payhubPaymentInstructionRepository,
            payhubFullRemissionRepository,
            auditRepository);
        paymentInstructionSearchCriteriaDtoBuilder = PaymentInstructionSearchCriteriaDto.paymentInstructionSearchCriteriaDto()
            .siteId("Y431");
        paymentInstructionStatusCriteriaDtoBuilder = PaymentInstructionStatusCriteriaDto.paymentInstructionStatusCriteriaDto();

        when(barUserMock.getSelectedSiteId()).thenReturn("Y431");

    }

    private void setUpValidator() {
        validatorService = new PaymentInstructionUpdateValidatorService(
            new ActionValidator(),
            new FullRemissionValidator(),
            new UnallocatedAmountValidator(unallocatedAmountService)
        );
    }

    @Test
    public void shouldReturnPaymentInstruction_whenSavePaymentInstructionForGivenChequeInstructionIsCalled()
        throws Exception {
        when(barUserMock.getRoles()).thenReturn(BAR_POST_CLERK_ROLE);
        when(paymentReferenceService.getNextPaymentReference(anyString()))
            .thenReturn(paymentReferenceMock);
        when(paymentReferenceMock.getSequenceId()).thenReturn(1);
        when(paymentInstructionRepository.saveAndFlush(any(ChequePaymentInstruction.class)))
            .thenReturn(chequePaymentInstructionMock);
        when(paymentInstructionRepository.saveAndRefresh(any(ChequePaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        when(paymentInstructionMock.getStatus()).thenReturn("status");

        PaymentInstruction createdPaymentInstruction = paymentInstructionServiceMock
            .createPaymentInstruction(barUserMock, chequePaymentInstructionMock);
        verify(paymentReferenceService, times(1)).getNextPaymentReference(anyString());
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(chequePaymentInstructionMock);
        PaymentInstructionStatus status = new PaymentInstructionStatus(null, createdPaymentInstruction);
        verify(paymentInstructionStatusRepositoryMock, times(1)).save(status);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("CREATE_PAYMENT_INSTRUCTION_EVENT",chequePaymentInstructionMock,barUserMock);
    }

    @Test
    public void shouldReturnPaymentInstruction_whenSavePaymentInstructionForGivenCashInstructionIsCalled()
        throws Exception {
        when(barUserMock.getRoles()).thenReturn(BAR_POST_CLERK_ROLE);
        when(paymentReferenceService.getNextPaymentReference(anyString()))
            .thenReturn(paymentReferenceMock);
        when(paymentInstructionRepository.saveAndFlush(any(CashPaymentInstruction.class)))
            .thenReturn(cashPaymentInstructionMock);
        when(paymentInstructionRepository.saveAndRefresh(any(CashPaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        when(paymentInstructionMock.getStatus()).thenReturn("status");
        PaymentInstruction createdPaymentInstruction = paymentInstructionServiceMock
            .createPaymentInstruction(barUserMock, cashPaymentInstructionMock);
        verify(paymentReferenceService, times(1)).getNextPaymentReference(anyString());
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(cashPaymentInstructionMock);
        PaymentInstructionStatus status = new PaymentInstructionStatus(null, createdPaymentInstruction);
        verify(paymentInstructionStatusRepositoryMock, times(1)).save(status);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("CREATE_PAYMENT_INSTRUCTION_EVENT",cashPaymentInstructionMock,barUserMock);


    }

    @Test
    public void shouldReturnPaymentInstruction_whenSavePaymentInstructionForGivenPostalOrderInstructionIsCalled()
        throws Exception {
        when(barUserMock.getRoles()).thenReturn(BAR_POST_CLERK_ROLE);
        when(paymentReferenceService.getNextPaymentReference(barUserMock.getSelectedSiteId()))
            .thenReturn(paymentReferenceMock);
        when(paymentInstructionRepository.saveAndFlush(any(PostalOrderPaymentInstruction.class)))
            .thenReturn(postalOrderPaymentInstructionMock);
        when(paymentInstructionRepository.saveAndRefresh(any(PostalOrderPaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        when(paymentInstructionMock.getStatus()).thenReturn("status");
        PaymentInstruction createdPaymentInstruction = paymentInstructionServiceMock
            .createPaymentInstruction(barUserMock, postalOrderPaymentInstructionMock);
        verify(paymentReferenceService, times(1)).getNextPaymentReference(anyString());
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(postalOrderPaymentInstructionMock);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("CREATE_PAYMENT_INSTRUCTION_EVENT",postalOrderPaymentInstructionMock,barUserMock);
    }

    @Test
    public void shouldReturnPaymentInstruction_whenSavePaymentInstructionForGivenAllPayInstructionIsCalled()
        throws Exception {
        when(barUserMock.getRoles()).thenReturn(BAR_POST_CLERK_ROLE);
        when(paymentReferenceService.getNextPaymentReference(anyString()))
            .thenReturn(paymentReferenceMock);
        when(paymentReferenceMock.getSequenceId()).thenReturn(1);
        when(paymentInstructionRepository.saveAndFlush(any(AllPayPaymentInstruction.class)))
            .thenReturn(allpayPaymentInstructionMock);
        when(paymentInstructionRepository.saveAndRefresh(any(AllPayPaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        when(paymentInstructionMock.getStatus()).thenReturn("status");


        PaymentInstruction createdPaymentInstruction = paymentInstructionServiceMock
            .createPaymentInstruction(barUserMock, allpayPaymentInstructionMock);
        verify(paymentReferenceService, times(1)).getNextPaymentReference(anyString());
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(allpayPaymentInstructionMock);
        PaymentInstructionStatus status = new PaymentInstructionStatus(null, createdPaymentInstruction);
        verify(paymentInstructionStatusRepositoryMock, times(1)).save(status);

        verify(auditRepository,times(1)).trackPaymentInstructionEvent("CREATE_PAYMENT_INSTRUCTION_EVENT",allpayPaymentInstructionMock,barUserMock);
    }

    @Test
    public void shouldReturnPaymentInstruction_whenSavePaymentInstructionForFeeClerkRoleIsCalled()
        throws Exception {
        when(barUserMock.getRoles()).thenReturn("bar-fee-clerk");
        when(paymentReferenceService.getNextPaymentReference(anyString()))
            .thenReturn(paymentReferenceMock);
        when(paymentReferenceMock.getSequenceId()).thenReturn(1);
        when(paymentInstructionRepository.saveAndFlush(any(AllPayPaymentInstruction.class)))
            .thenReturn(allpayPaymentInstructionMock);
        when(paymentInstructionRepository.saveAndRefresh(any(AllPayPaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        when(paymentInstructionMock.getStatus()).thenReturn("status");


        PaymentInstruction createdPaymentInstruction = paymentInstructionServiceMock
            .createPaymentInstruction(barUserMock, allpayPaymentInstructionMock);
        verify(paymentReferenceService, times(1)).getNextPaymentReference(anyString());
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(allpayPaymentInstructionMock);
        PaymentInstructionStatus status = new PaymentInstructionStatus(null, createdPaymentInstruction);
        verify(paymentInstructionStatusRepositoryMock, times(2)).save(status);

        verify(auditRepository,times(1)).trackPaymentInstructionEvent("CREATE_PAYMENT_INSTRUCTION_EVENT",allpayPaymentInstructionMock,barUserMock);
    }



    @Test
    public void shouldDeletePaymentInstruction_whenDeletePaymentInstructionIsCalled() throws Exception {
        when(paymentInstructionRepository.deleteByIdAndSiteId(anyInt(), anyString())).thenReturn(1);
        paymentInstructionServiceMock.deletePaymentInstruction(1, "Y431");

        verify(paymentInstructionRepository, times(1)).deleteByIdAndSiteId(1, "Y431");
    }

    @Test(expected = PaymentInstructionNotFoundException.class)
    public void shouldThrowPaymentInstructionNotFoundException_whenDeletePaymentInstructionIsCalledAndNotFound() {
        PaymentInstructionService service = mock(PaymentInstructionService.class);
        doThrow(PaymentInstructionNotFoundException.class).when(service).deletePaymentInstruction(1, "Y431");
        service.deletePaymentInstruction(1, "Y431");
    }

    @Test
    public void shouldNotDeletePaymentInstruction_whenSiteIdDoesNotMach() {
        when(paymentInstructionRepository.deleteByIdAndSiteId(anyInt(), anyString())).thenReturn(0);
        try{
            paymentInstructionServiceMock.deletePaymentInstruction(1, "Y431");
            fail("should fail here");
        } catch (PaymentInstructionNotFoundException e) {
            assertEquals("payment instruction: id = 1", e.getMessage());
        }
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithNoParams()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = PaymentInstructionSearchCriteriaDto
            .paymentInstructionSearchCriteriaDto().build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithAllParams()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .status("D").action("Suspense").startDate(LocalDate.now().atStartOfDay()).endDate(LocalDate.now().atTime(LocalTime.now()))
            .build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyStatus()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .status("D").build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyStartDate()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .startDate(LocalDate.now().atStartOfDay()).build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyEndDate()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .endDate(LocalDate.now().atTime(LocalTime.now())).build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyPayerName()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .payerName("Mr Payer Payer").build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyChequeNumber()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .chequeNumber("000000").build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldThrowError_whenPageNumberAndRecordsPerPageIsNotANumber()
        throws BadRequestException {

    	
        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .chequeNumber("000000")
            .pageNumber("abc")
            .recordsPerPage("abc").build();
          try
          {
        	 paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
            fail("should fail here");
          }
          catch(BadRequestException e)
          {
        	  assertEquals("Invalid PageNumber or Invalid MaxRecords per page", e.getMessage());
          }
    }
    
    @Test
    public void shouldReturnPaymentInstructionList_whenPageNumberAndRecordsPerPageIsNotNull()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .chequeNumber("000000")
            .pageNumber("0")
            .recordsPerPage("50").build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenPageNumberIsNotNull()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .chequeNumber("000000")
            .pageNumber("0")
            .build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }
    
    @Test
    public void shouldReturnPaymentInstructionList_whenRecordsPerPageIsNotNull()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .chequeNumber("000000")
            .recordsPerPage("50").build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }
    
    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyStartDateAndEndDate()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .startDate(LocalDate.now().atStartOfDay()).endDate(LocalDate.now().atTime(LocalTime.now())).build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyStatusAndEndDate()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .endDate(LocalDate.now().atTime(LocalTime.now())).build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyStatusAndStartDate()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .startDate(LocalDate.now().atStartOfDay()).build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsIsCalledWithOnlyDailySequenceId()
        throws Exception {

        when(paymentInstructionRepository.findAll(Mockito.any(Specification.class), Mockito.any(Pageable.class)))
            .thenReturn(piPageMock);
        when(piPageMock.iterator()).thenReturn(piIteratorMock);
        PaymentInstructionSearchCriteriaDto paymentInstructionSearchCriteriaDto = paymentInstructionSearchCriteriaDtoBuilder
            .dailySequenceId("1").build();

        List<PaymentInstruction> retrievedPaymentInstructionList = paymentInstructionService
            .getAllPaymentInstructions(barUserMock, paymentInstructionSearchCriteriaDto);
        assertEquals(Lists.newArrayList(piIteratorMock), retrievedPaymentInstructionList);
    }

    @Test
    public void shouldReturnPaymentInstruction_whenGetPaymentInstructionIsCalledForId() {
        Optional<PaymentInstruction> op = Optional.of(paymentInstructionMock);
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(op);

        PaymentInstruction pi = paymentInstructionService.getPaymentInstruction(3, "Y431");
        assertEquals(paymentInstructionMock, pi);
    }

    @Test
    public void shouldReturnNull_whenGetPaymentInstructionIsCalledForWrongId() {
        when(paymentInstructionRepository.getOne(Mockito.any(Integer.class))).thenReturn(null);

        PaymentInstruction pi = paymentInstructionService.getPaymentInstruction(3, "Y431");
        assertEquals(null, pi);
    }

    @Test
    public void shouldReturnSubmittedPaymentInstruction_whenSubmitPaymentInstructionForGivenPaymentInstructionIsCalled()
        throws Exception {
        ArgumentCaptor<PaymentInstruction> argument = ArgumentCaptor.forClass(PaymentInstruction.class);
        PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
            .status("D").build();
        PaymentInstruction existingPaymentInstruction = TestUtils.createPaymentInstructions("CHEQUE", 100);
        existingPaymentInstruction.setActionReason(2);
        existingPaymentInstruction.setActionComment("Valami tortent");
        existingPaymentInstruction.setAction("Process");
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(Optional.of(existingPaymentInstruction));
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenReturn(existingPaymentInstruction);
        PaymentInstruction updatedPaymentInstruction = paymentInstructionService.submitPaymentInstruction(barUserMock, 1, pir);
        verify(paymentInstructionRepository, times(1)).findByIdAndSiteId(anyInt(), anyString());
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(argument.capture());
        assertEquals("P", argument.getValue().getStatus());
        assertEquals(null, argument.getValue().getAction());
        assertEquals(null, argument.getValue().getActionComment());
        assertEquals(null, argument.getValue().getActionReason());
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",existingPaymentInstruction,barUserMock);
    }

    @Test
    public void shouldEnterNullForActionInformation_whenSubmitPaymentInstructionWithPendingStatusIsCalled()
        throws Exception {
        ArgumentCaptor<PaymentInstruction> argument = ArgumentCaptor.forClass(PaymentInstruction.class);
        PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
            .status("P").build();
        PaymentInstruction existingPaymentInstruction = TestUtils.createPaymentInstructions("CHEQUE", 10000);
        existingPaymentInstruction.setActionReason(2);
        existingPaymentInstruction.setActionComment("Valami tortent");
        existingPaymentInstruction.setAction("Process");
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(Optional.of(existingPaymentInstruction));
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenReturn(existingPaymentInstruction);
        PaymentInstruction updatedPaymentInstruction = paymentInstructionService.submitPaymentInstruction(barUserMock, 1, pir);
        assertEquals(null, updatedPaymentInstruction.getAction());
        assertEquals(null, updatedPaymentInstruction.getActionReason());
        assertEquals(null, updatedPaymentInstruction.getActionComment());
    }

    @Test
    public void shouldReturnSubmittedPaymentInstructionWithAction_whenSubmitPaymentInstructionForGivenPaymentInstructionIsCalledWithAction()
        throws Exception {
        when(paymentInstructionMock.getPaymentType()).thenReturn(PaymentType.paymentTypeWith().name("Cheque").id("CHEQUE").build());
        when(ff4jMock.check(PaymentActionEnum.SUSPENSE.featureKey())).thenReturn(true);
        PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
            .status("D").action("Suspense").build();
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(Optional.of(paymentInstructionMock));
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);

        paymentInstructionService.submitPaymentInstruction(barUserMock, 1, pir);
        verify(paymentInstructionRepository, times(1)).findByIdAndSiteId(anyInt(), anyString());
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(paymentInstructionMock);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",paymentInstructionMock,barUserMock);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",paymentInstructionMock,barUserMock);
        PaymentInstructionUpdateRequest anotherPir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
            .status("D").action("Process").build();
        try {
            paymentInstructionService.submitPaymentInstruction(barUserMock, 1, anotherPir);
            fail("should fail here");
        } catch (FeatureAccessException fae){
            assertEquals("Process is not allowed", fae.getMessage());
        }
    }

    @Test
    public void shouldRefuseSubmittedPaymentInstructionWithAction_whenSubmitPaymentInstructionForGivenPaymentInstructionIsCalledWithAction() throws PaymentProcessException {
        when(ff4jMock.check(anyString())).thenReturn(false);
        when(paymentInstructionRepository.findById(anyInt())).thenReturn(Optional.of(paymentInstructionMock));
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        try {
            PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
                .status("D").action("Suspense").build();
            paymentInstructionService.submitPaymentInstruction(barUserMock, 1, pir);
            fail("should fail here");
        } catch (FeatureAccessException fae){

            assertEquals("Suspense is not allowed", fae.getMessage());
        }
    }

    @Test
    public void shouldThrowPaymentProcessException_whenUnAllocatedAmountIsNotZero() {
        PaymentInstruction pi = TestUtils.createPaymentInstructions("CASH",10000);
        List<CaseFeeDetail> cfdList = new ArrayList<>();
        pi.setCaseFeeDetails(cfdList);
        when(paymentInstructionRepository.findByIdAndSiteId(any(Integer.class), anyString())).thenReturn(Optional.of(pi));
        when(ff4jMock.check(anyString())).thenReturn(true);
        try {
            PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
                .status("V").action("Process").build();
            paymentInstructionService.submitPaymentInstruction(barUserMock, 1, pir);
            fail("should fail here");
        } catch (PaymentProcessException ppe) {
            assertEquals("Please allocate all amount before processing.", ppe.getMessage());
        }
    }

    @Test
    public void shouldReturn200_whenUpdatePaymentInstructionForGivenPaymentInstructionIsCalled()
        throws Exception {
        PaymentInstructionRequest pir = PaymentInstructionRequest.paymentInstructionRequestWith()
            .amount(200)
            .payerName("Payer Name")
            .currency("GBP").build();
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(Optional.of(paymentInstructionMock));
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        when(paymentInstructionMock.getStatus()).thenReturn("status");
        paymentInstructionService.updatePaymentInstruction(barUserMock, 1, pir);
        verify(paymentInstructionRepository, times(1)).findByIdAndSiteId(anyInt(), eq("Y431"));
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(paymentInstructionMock);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",paymentInstructionMock,barUserMock);

    }

    @Test
    public void shouldReturn200andBgcUpdated_whenUpdatePostalInstructionWithBGCForGivenPaymentInstructionIsCalled() throws Exception{
        PaymentInstruction pi = new PostalOrderPaymentInstruction();
        PaymentInstructionRequest pir = PostalOrder.postalOrderPaymentInstructionRequestWith()
            .amount(200)
            .payerName("Payer Name")
            .currency("GBP")
            .bgcNumber("12345").build();
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), eq("Y431"))).thenReturn(Optional.of(pi));
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenAnswer(i -> i.getArguments()[0]);
        when(bankGiroCreditRepositoryMock.save(any(BankGiroCredit.class))).thenAnswer(i -> i.getArguments()[0]);
        // when(paymentInstructionMock.getStatus()).thenReturn("status");
        PaymentInstruction updatedPaymentInstruction = paymentInstructionService.updatePaymentInstruction(barUserMock, 1, pir);
        assertEquals(pir.getBgcNumber(), updatedPaymentInstruction.getBgcNumber());
        verify(paymentInstructionRepository, times(1)).findByIdAndSiteId(anyInt(), eq("Y431"));
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(pi);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",pi,barUserMock);

    }

    @Test
    public void shouldReturn200andBgcNotUpdated_whenUpdateCardPaymentInstructionWithBGCForGivenPaymentInstructionIsCalled() throws Exception{
        PaymentInstruction pi = new CardPaymentInstruction();
        PaymentInstructionRequest pir = PostalOrder.postalOrderPaymentInstructionRequestWith()
            .amount(200)
            .payerName("Payer Name")
            .currency("GBP")
            .bgcNumber("12345").build();
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), eq("Y431"))).thenReturn(Optional.of(pi));
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenAnswer(i -> i.getArguments()[0]);
        when(bankGiroCreditRepositoryMock.save(any(BankGiroCredit.class))).thenAnswer(i -> i.getArguments()[0]);
        // when(paymentInstructionMock.getStatus()).thenReturn("status");
        PaymentInstruction updatedPaymentInstruction = paymentInstructionService.updatePaymentInstruction(barUserMock, 1, pir);
        assertNull(updatedPaymentInstruction.getBgcNumber());
        verify(paymentInstructionRepository, times(1)).findByIdAndSiteId(anyInt(), eq("Y431"));
        verify(paymentInstructionRepository, times(1)).saveAndRefresh(pi);
        verify(auditRepository,times(1)).trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",pi,barUserMock);
    }

    @Test
    public void verifyRepositoryMethodCalls_whenGetPaymentInstructionStats() throws Exception {
        paymentInstructionService.getPaymentInstructionStats("",false, "Y431");
        verify(paymentInstructionStatusRepositoryMock, times(1))
            .getPaymentInstructionsByStatusGroupedByUser("", false, "Y431");
    }

    @Test
    public void verifyRepositoryMethodCalls_whenGetPaymentInstructionStatsByCurrentStatusGroupedByOldStatus()
        throws Exception {
        paymentInstructionService.getPaymentInstructionStatsByCurrentStatusGroupedByOldStatus("", "", "Y431");
        verify(paymentInstructionStatusRepositoryMock, times(1))
            .getPaymentInstructionStatsByCurrentStatusAndByOldStatus("", "", "Y431");
    }


    @Test
    public void shouldReturnEmptyPaymentInstructionList_whengetAllPaymentInstructionsByTTBWithIncorrectDates()
        throws Exception {

        List<PaymentInstruction> paymentInstructionList = paymentInstructionService.getAllPaymentInstructionsByTTB(LocalDate.now(), LocalDate.now().minusDays(1),"Y431");
        assertTrue(paymentInstructionList.isEmpty());
    }

    @Test
    public void testGettingPaymentInstructionStats_whenNoPayments() {
        when(paymentInstructionStatusRepositoryMock.getStatsByUserGroupByType(anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(new ArrayList<PaymentInstructionStats>());
        MultiMap stats = new MultiValueMap();
        assertEquals(stats, paymentInstructionService.getPaymentStatsByUserGroupByType("1234", "PA", Optional.empty(),false, "Y431"));
    }

    @Test
    public void testGettingPaymentInstructionStats() {
        List<PaymentInstructionStats> rawStats = createStats();
        when(paymentInstructionStatusRepositoryMock.getStatsByUserGroupByType(anyString(), anyString(), anyString() ,anyBoolean(), anyString())).thenReturn(rawStats);
        MultiMap stats = paymentInstructionService.getPaymentStatsByUserGroupByType("1234", "PA", Optional.empty(), false, "Y431");
        assertEquals(2, ((List)stats.get("bgc123")).size());
    }

    @Test
    public void testGettingPaymentInstructionStatsWhenSentToPayhub() {
        List<PaymentInstructionStats> rawStats = createStats();
        when(paymentInstructionStatusRepositoryMock.getStatsByUserGroupByType(anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(rawStats);
        MultiMap stats = paymentInstructionService.getPaymentStatsByUserGroupByType("1234", "PA", Optional.empty(), true, "Y431");
        assertEquals(2, ((List)stats.get("bgc123")).size());
    }

    @Test
    public void testCreatingLinksInTheStatResource() {
        List<PaymentInstructionStats> rawStats = createStats();
        when(paymentInstructionStatusRepositoryMock.getStatsByUserGroupByType(anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(rawStats);
        MultiMap stats = paymentInstructionService.getPaymentStatsByUserGroupByType("1234", "PA", Optional.empty(), false, "Y431");
        Resource<PaymentInstructionStats> resource = (Resource<PaymentInstructionStats>)((List)stats.get("bgc123")).get(0);
        assertEquals("/users/1234/payment-instructions?status=PA&paymentType=CHEQUE&action=Process&bgcNumber=bgc123", resource.getLink(STAT_DETAILS).getHref());
        assertEquals("/users/1234/payment-instructions?status=PA&paymentType=CHEQUE,POSTAL_ORDER&action=Process&bgcNumber=bgc123", resource.getLink(STAT_GROUP_DETAILS).getHref());
    }

    @Test
    public void testGetPaymentInstructionsByUserGroupByActionAndType() {
        List<PaymentInstructionStats> rawStats = createStats();
        when(paymentInstructionStatusRepositoryMock.getStatsByUserGroupByActionAndType(anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(rawStats);
        MultiMap stats = paymentInstructionService.getPaymentInstructionsByUserGroupByActionAndType("1234", "PA", Optional.empty(), false, "Y431");
        Resource<PaymentInstructionStats> resource = (Resource<PaymentInstructionStats>)((List)stats.get("bgc123")).get(0);
        assertEquals("/users/1234/payment-instructions?status=PA&paymentType=CHEQUE&action=Process&bgcNumber=bgc123", resource.getLink(STAT_DETAILS).getHref());
        assertEquals("/users/1234/payment-instructions?status=PA&paymentType=CHEQUE,POSTAL_ORDER&action=Process&bgcNumber=bgc123", resource.getLink(STAT_GROUP_DETAILS).getHref());
    }

    @Test
    public void testGetAllPaymentInstructionsForPayhub() {
        when(payhubPaymentInstructionRepository.findAll(Mockito.any(Specifications.class), Mockito.any(Pageable.class)))
            .thenReturn(payhubPiPageMock);
        when(payhubPiPageMock.iterator()).thenReturn(payhubPiIteratorMock);

        PaymentInstructionSearchCriteriaDto dto = new PaymentInstructionSearchCriteriaDto();
        dto.setStatus("TTB");
        dto.setTransferredToPayhub(false);

        List<PayhubPaymentInstruction> pis = paymentInstructionService.getAllPaymentInstructionsForPayhub(barUserMock, dto);
        assertEquals(Lists.newArrayList(payhubPiIteratorMock), pis);
    }

    @Test(expected = MissingSiteIdException.class)
    public void testGetAllPaymentInstructionsForPayhubWhenSiteIdNotProvided() {

        BarUser userWithMissingSite = BarUser.builder().email("nosite@mail.com").forename("no").surname("site").id("1").build();
        when(payhubPaymentInstructionRepository.findAll(Mockito.any(Specifications.class), Mockito.any(Pageable.class)))
            .thenReturn(payhubPiPageMock);
        when(payhubPiPageMock.iterator()).thenReturn(payhubPiIteratorMock);

        PaymentInstructionSearchCriteriaDto dto = new PaymentInstructionSearchCriteriaDto();
        dto.setStatus("TTB");
        dto.setTransferredToPayhub(false);

        List<PayhubPaymentInstruction> pis = paymentInstructionService.getAllPaymentInstructionsForPayhub(userWithMissingSite, dto);
        assertEquals(Lists.newArrayList(payhubPiIteratorMock), pis);
    }

    private List<PaymentInstructionStats> createStats() {
        List<PaymentInstructionStats> stats = new ArrayList<>();
        stats.add(createPaymentStat("James Black","1234", 1, "PA", 10000L, "CARD", null, "Process"));
        stats.add(createPaymentStat("James Black","1234", 4, "PA", 15000L, "CHEQUE", "bgc123", "Process"));
        stats.add(createPaymentStat("James Black","1234", 1, "PA", 33000L, "POSTAL_ORDER", "bgc123", "Process"));
        stats.add(createPaymentStat("James Black","1234", 1, "PA", 10000L, "CASH", "bgc456", "Process"));
        return stats;
    }

    private PaymentInstructionStats createPaymentStat(String name, String userId, Integer count, String status,
                                                      Long totalAmount, String paymentType, String bgc, String action) {
        return new PaymentInstructionStats() {
            @Override
            public String getName() { return name; }
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public Integer getCount() {
                return count;
            }

            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public Long getTotalAmount() {
                return totalAmount;
            }

            @Override
            public String getPaymentType() {
                return paymentType;
            }

            @Override
            public String getBgc() {
                return bgc;
            }

            @Override
            public String getAction() {
                return action;
            }

        };
    }

    @Test
    public void shouldReturnPaymentInstructionList_whenGetAllPaymentInstructionsByCaseReferenceIsCalled() {
        List<PaymentInstruction> piList = Arrays.asList(paymentInstructionMock);
        when(paymentInstructionServiceMock.getAllPaymentInstructionsByCaseReference("")).thenReturn(piList);
        List<PaymentInstruction> paymentInstructionList = paymentInstructionServiceMock
            .getAllPaymentInstructionsByCaseReference("");
        assertFalse(paymentInstructionList.isEmpty());
    }

    @Test
    public void shouldReturnEmptyMap_whenGetStatusHistortMapForTTBCalledWithStartdateGreaterThanEndDate()
        throws Exception {
        Map<Integer, List<PaymentInstructionStatusHistory>> pishMap = paymentInstructionService
            .getStatusHistoryMapForTTB(LocalDate.now(), LocalDate.now().minusDays(1),"Y431");
        assertTrue(pishMap.isEmpty());
    }

    @Test
    public void shouldReturnPICount_whenGetPICountIsCalled() {

        when(paymentInstructionStatusRepositoryMock.count(Mockito.any(Specification.class)))
            .thenReturn(1L);
        PaymentInstructionStatusCriteriaDto paymentInstructionStatusCriteriaDto = paymentInstructionStatusCriteriaDtoBuilder
            .status("PA").build();

        long count = paymentInstructionService
            .getPaymentInstructionsCount(paymentInstructionStatusCriteriaDto);
        assertEquals(1, count);
    }

    @Test
    public void shouldReturnNonResetPICount_whenGetNonResetPaymentInstructionsCountIsCalled() {

        when(paymentInstructionStatusRepositoryMock.getNonResetCountByStatus(anyString(), eq("Y431")))
            .thenReturn(1L);

        long count = paymentInstructionService
            .getNonResetPaymentInstructionsCount("D", "Y431");
        assertEquals(1, count);
    }

    @Test
    public void returnShouldThrowExceptionWhenCaseFeeAttached() {
        when(ff4jMock.check(PaymentActionEnum.RETURN.featureKey())).thenReturn(true);
        PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
            .status("A").action("Return").build();
        PaymentInstruction existingPaymentInstruction = TestUtils.createPaymentInstructions("CHEQUE", 10000);
        existingPaymentInstruction.setCaseFeeDetails(Arrays.asList(new CaseFeeDetail()));
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(Optional.of(existingPaymentInstruction));
        try {
            paymentInstructionService.submitPaymentInstruction(barUserMock, 1, pir);
            fail("should fail here");
        } catch (PaymentProcessException e) {
            assertEquals("Please remove all case and fee details before attempting this action.", e.getMessage());
        }
    }

    @Test
    public void withdrawShouldThrowExceptionWhenCaseFeeAttached() {
        when(ff4jMock.check(PaymentActionEnum.WITHDRAW.featureKey())).thenReturn(true);
        PaymentInstructionUpdateRequest pir = PaymentInstructionUpdateRequest.paymentInstructionUpdateRequestWith()
            .status("A").action("Withdraw").build();
        PaymentInstruction existingPaymentInstruction = new ChequePaymentInstruction();
        existingPaymentInstruction.setPaymentType(PaymentType.paymentTypeWith().id("CHEQUE").name("Cheque").build());
        existingPaymentInstruction.setCaseFeeDetails(Arrays.asList(new CaseFeeDetail()));
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(Optional.of(existingPaymentInstruction));
        try {
            paymentInstructionService.submitPaymentInstruction(barUserMock, 1, pir);
            fail("should fail here");
        } catch (PaymentProcessException e) {
            assertEquals("Please remove all case and fee details before attempting this action.", e.getMessage());
        }
    }

    @Test
    public void testUpdatePaymentWithDifferentSiteIdWhatTheUserHas() {
        PaymentInstructionRequest pir = PaymentInstructionRequest.paymentInstructionRequestWith()
            .amount(200)
            .payerName("Payer Name")
            .currency("GBP").build();
        when(paymentInstructionRepository.findByIdAndSiteId(anyInt(), anyString())).thenReturn(Optional.empty());
        when(paymentInstructionRepository.saveAndRefresh(any(PaymentInstruction.class)))
            .thenReturn(paymentInstructionMock);
        when(paymentInstructionMock.getStatus()).thenReturn("status");
        try {
            paymentInstructionService.updatePaymentInstruction(barUserMock, 1, pir);
            fail("Should fail here, as no payment found");
        } catch (Exception e) {
            assertEquals("payment instruction on site Y431: id = 1", e.getMessage());
        }
        verify(paymentInstructionRepository, times(1)).findByIdAndSiteId(anyInt(), eq("Y431"));
        verify(paymentInstructionRepository, times(0)).saveAndRefresh(paymentInstructionMock);
        verify(auditRepository,times(0)).trackPaymentInstructionEvent("PAYMENT_INSTRUCTION_UPDATE_EVENT",paymentInstructionMock,barUserMock);

    }

}
