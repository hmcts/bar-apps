package uk.gov.hmcts.bar.api.data.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.bar.api.data.model.PaymentInstruction;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentInstructionRepository extends BaseRepository<PaymentInstruction, Integer>, JpaSpecificationExecutor<PaymentInstruction> {
    Optional<PaymentInstruction>  findById(Integer id);

    @Query("SELECT pi FROM PaymentInstruction pi, CaseFeeDetail cfd  WHERE " +
            " cfd.paymentInstructionId = pi.id AND cfd.caseReference like %:caseReference%")
    List<PaymentInstruction> findByCaseReference(@Param("caseReference") String caseReference);

    @Modifying
    @Query(value = "UPDATE payment_instruction SET transferred_to_payhub = :status, payhub_error = :errorMessage " +
        "WHERE id = :id", nativeQuery = true)
    int updateTransferredToPayHub(@Param("id") int id, @Param("status") boolean status, @Param("errorMessage") String errorMessage);
}
