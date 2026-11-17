package com.iare.club.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iare.club.entity.AppUser;
import com.iare.club.entity.Deposit;
import com.iare.club.service.DepositService;
import com.iare.club.service.PaymentOriginService;
import com.iare.club.service.UserService;
import com.iare.club.service.ZapUpiService;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/payments/zapupi")
public class ZapUpiController {
    private static final Logger log = LoggerFactory.getLogger(ZapUpiController.class);
    private final ZapUpiService zapUpi; private final UserService users; private final DepositService deposits; private final PaymentOriginService origins;
    public ZapUpiController(ZapUpiService zapUpi, UserService users, DepositService deposits, PaymentOriginService origins) { this.zapUpi=zapUpi; this.users=users; this.deposits=deposits; this.origins=origins; }

    @GetMapping("/checkout/{orderId}")
    public RedirectView checkout(@PathVariable String orderId,Authentication auth,Model model){
        Deposit deposit=deposits.findForUser(orderId,users.findByEmail(auth.getName()));
        if(deposit.getStatus()==Deposit.Status.PENDING&&deposit.getGatewayCheckoutUrl()!=null&&deposit.getGatewayCheckoutUrl().startsWith("https://pay.zapupi.com/"))return new RedirectView(deposit.getGatewayCheckoutUrl());
        return new RedirectView("/payments/zapupi/status-page/"+URLEncoder.encode(orderId,StandardCharsets.UTF_8));
    }

    @GetMapping("/status-page/{orderId}")
    public String statusPage(@PathVariable String orderId,Authentication auth,Model model){model.addAttribute("deposit",deposits.findForUser(orderId,users.findByEmail(auth.getName())));return "finance/zapupi-checkout";}

    @GetMapping(value="/checkout/{orderId}/qr",produces="image/png") @ResponseBody
    public ResponseEntity<byte[]> qr(@PathVariable String orderId,Authentication auth){
        Deposit deposit=deposits.findForUser(orderId,users.findByEmail(auth.getName()));
        String data=deposit.getGatewayPaymentPayload();
        if(data==null||data.isBlank())return ResponseEntity.notFound().build();
        try{var matrix=new QRCodeWriter().encode(data,BarcodeFormat.QR_CODE,420,420);ByteArrayOutputStream out=new ByteArrayOutputStream();MatrixToImageWriter.writeToStream(matrix,"PNG",out);return ResponseEntity.ok().header("Cache-Control","no-store").body(out.toByteArray());}catch(Exception e){throw new IllegalStateException("Unable to render payment QR",e);}
    }

    @GetMapping("/check/{orderId}") @ResponseBody
    public ResponseEntity<PaymentState> check(@PathVariable String orderId,Authentication auth){
        AppUser user=users.findByEmail(auth.getName());ZapUpiService.Result result;
        try{result=zapUpi.reconcileForUser(orderId,user);}catch(RuntimeException e){log.warn("ZapUPI checkout polling deferred for order {}: {}",orderId,e.getMessage());Deposit current=deposits.findForUser(orderId,user);result=new ZapUpiService.Result(orderId,current.getStatus(),"Verification delayed");}
        Deposit current=deposits.findForUser(orderId,user);return ResponseEntity.ok(new PaymentState(result.orderId(),result.status().name(),result.message(),current.getAmount(),current.getGatewayTransactionId(),current.getWalletBalanceAfter(),current.getPaymentPurpose()));
    }

    @GetMapping("/return")
    public RedirectView returned(@RequestParam(name="order_id") String orderId, Authentication auth) {
        return new RedirectView(returnTarget(safeReconcileForUser(orderId, auth)));
    }

    @GetMapping("/status")
    public RedirectView status(@RequestParam(name="order_id") String orderId, Authentication auth) {
        return new RedirectView(returnTarget(safeReconcileForUser(orderId, auth)));
    }

    @GetMapping("/result") @ResponseBody
    public ResponseEntity<VerifiedPayment> result(@RequestParam(name="order_id") String orderId, Authentication auth) {
        return deposits.findSuccessfulForUser(orderId, users.findByEmail(auth.getName()))
                .map(value -> ResponseEntity.ok(new VerifiedPayment(value.getAmount(), value.getWalletBalanceAfter(), value.getPaymentReference(), value.getGatewayTransactionId(), value.getDisplayMethod())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/webhook") @ResponseBody
    public ResponseEntity<Map<String,String>> webhook(@RequestBody(required=false) JsonNode payload) {
        String orderId = payload == null ? null : text(payload, "order_id");
        if (orderId != null && orderId.matches("ZAP[A-Z0-9]{24}")) {
            log.info("ZapUPI webhook received orderId={}", orderId);
            try { ZapUpiService.Result result=zapUpi.reconcile(orderId); log.info("ZapUPI webhook reconciled orderId={} status={}",orderId,result.status()); }
            catch (RuntimeException e) { log.warn("ZapUPI webhook verification deferred for order {}: {}", orderId, e.getMessage()); }
        } else log.warn("Ignored malformed ZapUPI webhook");
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private ZapUpiService.Result safeReconcileForUser(String orderId, Authentication auth) {
        try { return zapUpi.reconcileForUser(orderId, users.findByEmail(auth.getName())); }
        catch (RuntimeException e) { log.warn("ZapUPI status verification deferred for order {}: {}", orderId, e.getMessage()); return new ZapUpiService.Result(orderId, Deposit.Status.PENDING, "Payment confirmation is still pending"); }
    }
    private String returnTarget(ZapUpiService.Result result) {
        String origin = deposits.returnOrigin(result.orderId()).map(origins::safeStoredOrigin).orElseGet(() -> origins.safeStoredOrigin(null));
        String id=URLEncoder.encode(result.orderId(),StandardCharsets.UTF_8);
        boolean product=deposits.paymentPurpose(result.orderId()).map("PRODUCT_PURCHASE"::equals).orElse(false);
        if(result.status()==Deposit.Status.SUCCESSFUL&&!product)return origin+"/deposit?payment=success&order_id="+id;
        if((result.status()==Deposit.Status.FAILED||result.status()==Deposit.Status.CANCELLED)&&!product)return origin+"/deposit?payment=failed&order_id="+id;
        return origin+"/payments/zapupi/status-page/"+id;
    }
    private static String text(JsonNode n,String field){JsonNode value=n.get(field);return value==null||value.isNull()?null:value.asText();}
    public record VerifiedPayment(BigDecimal amount, BigDecimal walletBalance, String merchantOrderId, String zapUpiTransactionId, String paymentMethod) {}
    public record PaymentState(String orderId,String status,String message,BigDecimal amount,String transactionId,BigDecimal walletBalance,String purpose) {}
}
