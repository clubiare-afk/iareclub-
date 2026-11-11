package com.iare.club.service;

import com.iare.club.entity.AppUser;
import com.iare.club.entity.Deposit;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PayUService {
    private static final String PRODUCT="Money Mint Deposit";
    private final DepositService deposits;private final PayUClient client;private final PayUHashService hashes;private final boolean enabled;private final String key;private final String checkoutUrl;private final String callbackUrl;private final String customerPhone;
    public PayUService(DepositService deposits,PayUClient client,PayUHashService hashes,@Value("${payu.enabled:false}")boolean enabled,@Value("${payu.environment:TEST}")String environment,@Value("${payu.merchant-key:}")String key,@Value("${payu.salt:}")String salt,@Value("${payu.public-base-url:http://localhost:8080}")String base,@Value("${payu.customer-phone:9999999999}")String customerPhone){
        this.deposits=deposits;this.client=client;this.hashes=hashes;this.enabled=enabled;this.key=key.strip();this.customerPhone=customerPhone.strip();
        if(enabled&&(this.key.isBlank()||salt.isBlank()))throw new IllegalStateException("PayU is enabled but PAYU_MERCHANT_KEY or PAYU_SALT is missing");
        checkoutUrl="PRODUCTION".equalsIgnoreCase(environment)?"https://secure.payu.in/_payment":"https://test.payu.in/_payment";
        callbackUrl=base.replaceAll("/$","")+"/payments/payu/callback";
    }
    public Checkout initiate(AppUser user,java.math.BigDecimal amount){
        if(!enabled)throw new DepositService.FinancialException("Online payments are not enabled yet");
        Deposit deposit=deposits.createPayU(user,amount);String formatted=deposit.getAmount().setScale(2,RoundingMode.UNNECESSARY).toPlainString();String first=user.getFullName().strip().split("\\s+",2)[0];String udf1=user.getId().toString();
        Map<String,String> fields=new LinkedHashMap<>();fields.put("key",key);fields.put("txnid",deposit.getPaymentReference());fields.put("amount",formatted);fields.put("productinfo",PRODUCT);fields.put("firstname",first);fields.put("email",user.getEmail());fields.put("phone",customerPhone);fields.put("surl",callbackUrl);fields.put("furl",callbackUrl);fields.put("curl",callbackUrl);fields.put("udf1",udf1);fields.put("udf2","");fields.put("udf3","");fields.put("udf4","");fields.put("udf5","");fields.put("hash",hashes.paymentHash(deposit.getPaymentReference(),formatted,PRODUCT,first,user.getEmail(),udf1));
        return new Checkout(checkoutUrl,fields,deposit.getPaymentReference());
    }
    public Result processCallback(Map<String,String> payload){
        String transaction=payload.get("txnid");if(transaction==null||!hashes.validResponse(payload))return new Result(transaction,Deposit.Status.PENDING,"Invalid PayU response signature; payment was not credited");
        return reconcile(transaction);
    }
    public Result reconcile(String transaction){PayUClient.Verification verified=client.verify(transaction);Deposit deposit=deposits.applyPayUVerification(verified);return new Result(transaction,deposit.getStatus(),deposit.getGatewayMessage());}
    public record Checkout(String action,Map<String,String> fields,String transactionId){}
    public record Result(String transactionId,Deposit.Status status,String message){}
}
