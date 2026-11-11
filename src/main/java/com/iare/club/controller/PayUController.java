package com.iare.club.controller;

import com.iare.club.service.PayUService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.*;
import org.springframework.security.core.Authentication;
import com.iare.club.service.UserService;
import com.iare.club.service.DepositService;
import com.iare.club.service.PaymentOriginService;
import com.iare.club.entity.Deposit;
import java.math.BigDecimal;

@Controller
@RequestMapping("/payments/payu")
public class PayUController {
    private static final Logger log=LoggerFactory.getLogger(PayUController.class);
    private final PayUService payu;private final UserService users;private final DepositService deposits;private final PaymentOriginService paymentOrigins;
    public PayUController(PayUService payu,UserService users,DepositService deposits,PaymentOriginService paymentOrigins){this.payu=payu;this.users=users;this.deposits=deposits;this.paymentOrigins=paymentOrigins;}
    @PostMapping("/callback") public ModelAndView callback(@RequestParam Map<String,String> payload){
        PayUService.Result result=safeProcess(payload);
        return new ModelAndView("finance/payu-return","returnUrl",returnTarget(result));
    }
    @GetMapping("/status") public RedirectView status(@RequestParam String txnid,Authentication auth){return redirect(safeReconcileForUser(txnid,auth));}
    @GetMapping("/result") @ResponseBody public ResponseEntity<VerifiedPayment> result(@RequestParam String txnid,Authentication auth){return deposits.findSuccessfulForUser(txnid,users.findByEmail(auth.getName())).map(value->ResponseEntity.ok(new VerifiedPayment(value.getAmount(),value.getWalletBalanceAfter(),value.getPaymentReference(),value.getDisplayPaymentId(),value.getDisplayMethod()))).orElseGet(()->ResponseEntity.notFound().build());}
    @PostMapping("/webhook") @ResponseBody public ResponseEntity<String> webhook(@RequestParam Map<String,String> payload){if(!payu.validCallback(payload))return ResponseEntity.badRequest().body("invalid signature");PayUService.Result result=safeProcess(payload);return result.transactionId()==null?ResponseEntity.badRequest().body("invalid transaction"):ResponseEntity.ok("accepted");}
    private PayUService.Result safeProcess(Map<String,String> payload){try{return payu.processCallback(payload);}catch(RuntimeException e){log.warn("PayU callback verification deferred for transaction {}: {}",payload.get("txnid"),e.getMessage());return pending(payload.get("txnid"));}}
    private PayUService.Result safeReconcile(String transaction){try{return payu.reconcile(transaction);}catch(RuntimeException e){log.warn("PayU status verification deferred for transaction {}: {}",transaction,e.getMessage());return pending(transaction);}}
    private PayUService.Result safeReconcileForUser(String transaction,Authentication auth){try{return payu.reconcileForUser(transaction,users.findByEmail(auth.getName()));}catch(RuntimeException e){log.warn("PayU status verification deferred for transaction {}: {}",transaction,e.getMessage());return pending(transaction);}}
    private PayUService.Result pending(String transaction){return new PayUService.Result(transaction,com.iare.club.entity.Deposit.Status.PENDING,"PayU is still confirming this payment. This page will check again automatically.");}
    private RedirectView redirect(PayUService.Result result){
        return new RedirectView(returnTarget(result));
    }
    private String returnTarget(PayUService.Result result){
        String payment=result.status()==com.iare.club.entity.Deposit.Status.SUCCESSFUL?"success":result.status()==com.iare.club.entity.Deposit.Status.PENDING?"pending":"failed";
        String origin=deposits.returnOrigin(result.transactionId()).map(paymentOrigins::safeStoredOrigin).orElseGet(()->paymentOrigins.safeStoredOrigin(null));
        String target=origin+"/deposit?payment="+payment;
        if(result.transactionId()!=null)target+="&txnid="+java.net.URLEncoder.encode(result.transactionId(),java.nio.charset.StandardCharsets.UTF_8);
        return target;
    }
    public record VerifiedPayment(BigDecimal amount,BigDecimal walletBalance,String merchantTransactionId,String payuTransactionId,String paymentMethod){}
}
