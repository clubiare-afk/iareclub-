package com.iare.club.controller;

import com.iare.club.service.PayUService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.slf4j.*;

@Controller
@RequestMapping("/payments/payu")
public class PayUController {
    private static final Logger log=LoggerFactory.getLogger(PayUController.class);
    private final PayUService payu;
    public PayUController(PayUService payu){this.payu=payu;}
    @PostMapping("/callback") public RedirectView callback(@RequestParam Map<String,String> payload){return redirect(safeProcess(payload));}
    @GetMapping("/status") public RedirectView status(@RequestParam String txnid){return redirect(safeReconcile(txnid));}
    @PostMapping("/webhook") @ResponseBody public ResponseEntity<String> webhook(@RequestParam Map<String,String> payload){PayUService.Result result=safeProcess(payload);return result.transactionId()==null?ResponseEntity.badRequest().body("invalid"):ResponseEntity.ok("accepted");}
    private PayUService.Result safeProcess(Map<String,String> payload){try{return payu.processCallback(payload);}catch(RuntimeException e){log.warn("PayU callback verification deferred for transaction {}: {}",payload.get("txnid"),e.getMessage());return pending(payload.get("txnid"));}}
    private PayUService.Result safeReconcile(String transaction){try{return payu.reconcile(transaction);}catch(RuntimeException e){log.warn("PayU status verification deferred for transaction {}: {}",transaction,e.getMessage());return pending(transaction);}}
    private PayUService.Result pending(String transaction){return new PayUService.Result(transaction,com.iare.club.entity.Deposit.Status.PENDING,"PayU is still confirming this payment. This page will check again automatically.");}
    private RedirectView redirect(PayUService.Result result){
        String payment=result.status()==com.iare.club.entity.Deposit.Status.SUCCESSFUL?"success":result.status()==com.iare.club.entity.Deposit.Status.PENDING?"pending":"failed";
        String target="/deposit?payment="+payment;
        if("pending".equals(payment)&&result.transactionId()!=null)target+="&txnid="+java.net.URLEncoder.encode(result.transactionId(),java.nio.charset.StandardCharsets.UTF_8);
        return new RedirectView(target,true);
    }
}
