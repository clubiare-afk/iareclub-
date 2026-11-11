package com.iare.club.service;
import com.iare.club.repository.DepositRepository;
import com.iare.club.entity.Deposit;
import java.time.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class PayUReconciliationJob {
 private static final Logger log=LoggerFactory.getLogger(PayUReconciliationJob.class);private final DepositRepository deposits;private final PayUService payu;private final Clock clock;private final boolean enabled;
 public PayUReconciliationJob(DepositRepository deposits,PayUService payu,Clock clock,@Value("${payu.enabled:false}")boolean payuEnabled,@Value("${payu.reconciliation.enabled:true}")boolean reconciliation){this.deposits=deposits;this.payu=payu;this.clock=clock;enabled=payuEnabled&&reconciliation;}
 @Scheduled(fixedDelayString="${payu.reconciliation.fixed-delay-ms:300000}") public void reconcile(){if(!enabled)return;for(String ref:deposits.findPendingReferences(Deposit.Status.PENDING,clock.instant().minus(Duration.ofMinutes(2)),PageRequest.of(0,50)))try{payu.reconcile(ref);}catch(RuntimeException e){log.warn("PayU reconciliation deferred for transaction {}: {}",ref,e.getMessage());}}
}
