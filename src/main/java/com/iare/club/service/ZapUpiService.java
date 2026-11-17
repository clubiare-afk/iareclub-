package com.iare.club.service;

import com.iare.club.entity.AppUser;
import com.iare.club.entity.Deposit;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ZapUpiService {
    private static final Logger log = LoggerFactory.getLogger(ZapUpiService.class);
    private final DepositService deposits;
    private final ZapUpiClient client;
    private final boolean enabled;
    private final boolean liveMode;
    private final String publicBaseUrl;

    public ZapUpiService(DepositService deposits, ZapUpiClient client,
            @Value("${zapupi.enabled:false}") boolean enabled,
            @Value("${zapupi.live-mode:false}") boolean liveMode,
            @Value("${zapupi.public-base-url:}") String publicBaseUrl) {
        this.deposits = deposits; this.client = client; this.enabled = enabled; this.liveMode = liveMode;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        if (enabled && !client.isConfigured()) throw new IllegalStateException("ZapUPI is enabled but ZAPUPI_API_KEY is missing");
        if (enabled && !this.publicBaseUrl.startsWith("https://")) throw new IllegalStateException("ZapUPI requires a public HTTPS base URL");
    }

    public Checkout initiate(AppUser user, BigDecimal amount, String returnOrigin) {
        if (!enabled) throw new DepositService.FinancialException("Online payments are temporarily unavailable");
        var reusable = deposits.reusableZapUpi(user, amount);
        if (reusable.isPresent()) {
            log.info("Reusing pending ZapUPI order orderId={} userId={} amount={}", reusable.get().getPaymentReference(), user.getId(), reusable.get().getAmount());
            return new Checkout(reusable.get().getPaymentReference(), reusable.get().getGatewayCheckoutUrl());
        }
        Deposit deposit = deposits.createZapUpi(user, amount, returnOrigin);
        log.info("Creating ZapUPI order orderId={} userId={} amount={} callbackOrigin={}", deposit.getPaymentReference(), user.getId(), deposit.getAmount(), publicBaseUrl);
        String returnUrl = browserReturnUrl(returnOrigin);
        ZapUpiClient.CreatedOrder order = client.createOrder(deposit.getPaymentReference(), deposit.getAmount(),
                "FundRise wallet | user:" + user.getId(), returnUrl, returnUrl, returnUrl,
                publicBaseUrl + "/payments/zapupi/webhook");
        String paymentPayload=order.encodedPaymentData()==null?null:java.net.URLDecoder.decode(order.encodedPaymentData(),java.nio.charset.StandardCharsets.UTF_8);
        deposits.recordZapUpiOrder(deposit.getPaymentReference(), order.transactionId(), order.environment(),order.paymentUrl(),order.payId(),paymentPayload);
        return new Checkout(order.orderId(),order.paymentUrl());
    }

    public Checkout initiateProduct(AppUser user,long productId,String requestReference,String returnOrigin){
        if(!enabled)throw new DepositService.FinancialException("Online payments are temporarily unavailable");
        Deposit payment=deposits.createZapUpiProduct(user,productId,requestReference,returnOrigin);
        String returnUrl=browserReturnUrl(returnOrigin);
        log.info("Creating ZapUPI product order orderId={} userId={} productId={} amount={}",payment.getPaymentReference(),user.getId(),productId,payment.getAmount());
        ZapUpiClient.CreatedOrder order=client.createOrder(payment.getPaymentReference(),payment.getAmount(),"FundRise product | user:"+user.getId()+" | product:"+productId,returnUrl,returnUrl,returnUrl,publicBaseUrl+"/payments/zapupi/webhook");
        String payload=order.encodedPaymentData()==null?null:java.net.URLDecoder.decode(order.encodedPaymentData(),java.nio.charset.StandardCharsets.UTF_8);
        deposits.recordZapUpiOrder(payment.getPaymentReference(),order.transactionId(),order.environment(),order.paymentUrl(),order.payId(),payload);
        return new Checkout(order.orderId(),order.paymentUrl());
    }

    public Result reconcile(String orderId) {
        ZapUpiClient.Verification verification = client.orderStatus(orderId);
        log.info("ZapUPI verification orderId={} status={} environment={} amount={} paidAmount={} txnPresent={} utrPresent={}",orderId,verification.status(),verification.environment(),verification.amount(),verification.paidAmount(),verification.transactionId()!=null&&!verification.transactionId().isBlank(),verification.utr()!=null&&!verification.utr().isBlank());
        Deposit deposit = deposits.applyZapUpiVerification(verification, liveMode);
        return new Result(orderId, deposit.getStatus(), deposit.getGatewayMessage());
    }

    public Result reconcileForUser(String orderId, AppUser user) {
        deposits.findForUser(orderId, user);
        return reconcile(orderId);
    }

    public boolean isEnabled() { return enabled; }
    private static String browserReturnUrl(String origin){
        java.net.URI uri;try{uri=java.net.URI.create(origin);}catch(Exception e){throw new DepositService.FinancialException("Invalid payment return origin");}
        boolean local="http".equalsIgnoreCase(uri.getScheme())&&"localhost".equalsIgnoreCase(uri.getHost());
        if((!"https".equalsIgnoreCase(uri.getScheme())&&!local)||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||uri.getPath()!=null&&!uri.getPath().isEmpty())throw new DepositService.FinancialException("Invalid payment return origin");
        return origin.replaceAll("/+$","")+"/payments/zapupi/return";
    }
    private static String validateDirectUpiPayload(String payload,BigDecimal expectedAmount,String payId){
        if(payId==null||payId.isBlank()||payload==null||payload.isBlank())throw new DepositService.FinancialException("ZapUPI Custom API Feature is required for direct UPI QR payments");
        java.net.URI uri;try{uri=java.net.URI.create(payload.replace(" ","%20"));}catch(IllegalArgumentException e){throw new DepositService.FinancialException("ZapUPI returned an invalid UPI payment payload");}
        if(!"upi".equalsIgnoreCase(uri.getScheme())||!"pay".equalsIgnoreCase(uri.getHost()))throw new DepositService.FinancialException("ZapUPI did not return a direct UPI payment QR");
        java.util.Map<String,String> query=new java.util.HashMap<>();if(uri.getRawQuery()!=null)for(String item:uri.getRawQuery().split("&")){String[] pair=item.split("=",2);query.put(java.net.URLDecoder.decode(pair[0],java.nio.charset.StandardCharsets.UTF_8),pair.length==2?java.net.URLDecoder.decode(pair[1],java.nio.charset.StandardCharsets.UTF_8):"");}
        if(query.get("pa")==null||query.get("pa").isBlank()||query.get("tr")==null||query.get("tr").isBlank())throw new DepositService.FinancialException("ZapUPI UPI QR is missing merchant or transaction details");
        try{if(query.get("am")==null||new BigDecimal(query.get("am")).compareTo(expectedAmount)!=0)throw new DepositService.FinancialException("ZapUPI UPI QR amount does not match the deposit");}catch(NumberFormatException e){throw new DepositService.FinancialException("ZapUPI UPI QR contains an invalid amount");}
        return uri.toASCIIString();
    }
    public record Checkout(String orderId, String paymentUrl) {}
    public record Result(String orderId, Deposit.Status status, String message) {}
}
