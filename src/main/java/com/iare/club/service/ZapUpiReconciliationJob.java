package com.iare.club.service;

import com.iare.club.entity.Deposit;
import com.iare.club.repository.DepositRepository;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ZapUpiReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(ZapUpiReconciliationJob.class);
    private final DepositRepository deposits; private final ZapUpiService zapUpi; private final Clock clock; private final boolean enabled;
    public ZapUpiReconciliationJob(DepositRepository deposits, ZapUpiService zapUpi, Clock clock,
            @Value("${zapupi.reconciliation.enabled:true}") boolean reconciliationEnabled) {
        this.deposits=deposits; this.zapUpi=zapUpi; this.clock=clock; this.enabled=zapUpi.isEnabled() && reconciliationEnabled;
    }
    @Scheduled(fixedDelayString="${zapupi.reconciliation.fixed-delay-ms:30000}")
    public void reconcile() {
        if (!enabled) return;
        for (String ref : deposits.findPendingReferencesByMethodAndCreatedBefore(Deposit.Status.PENDING, "ZAPUPI", clock.instant().minus(Duration.ofSeconds(10)), PageRequest.of(0, 100)))
            try { zapUpi.reconcile(ref); } catch (RuntimeException e) { log.warn("ZapUPI reconciliation deferred for order {}: {}", ref, e.getMessage()); }
    }
}
