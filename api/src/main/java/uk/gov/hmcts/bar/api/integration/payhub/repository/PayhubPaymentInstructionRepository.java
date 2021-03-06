package uk.gov.hmcts.bar.api.integration.payhub.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uk.gov.hmcts.bar.api.data.repository.BaseRepository;
import uk.gov.hmcts.bar.api.integration.payhub.data.PayhubPaymentInstruction;

public interface PayhubPaymentInstructionRepository extends BaseRepository<PayhubPaymentInstruction, Integer>, JpaSpecificationExecutor<PayhubPaymentInstruction> {
}
