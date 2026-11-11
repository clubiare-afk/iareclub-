package com.iare.club.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PayUClient {
    private final HttpClient client; private final ObjectMapper json; private final PayUHashService hashes; private final String key; private final String endpoint; private final Duration readTimeout;
    public PayUClient(ObjectMapper json,PayUHashService hashes,@Value("${payu.environment:TEST}")String environment,@Value("${payu.merchant-key:}")String key,@Value("${payu.connect-timeout:5s}")Duration connect,@Value("${payu.read-timeout:15s}")Duration read){
        this.json=json;this.hashes=hashes;this.key=key.strip();this.readTimeout=read;
        endpoint="PRODUCTION".equalsIgnoreCase(environment)?"https://info.payu.in/merchant/postservice.php?form=2":"https://test.payu.in/merchant/postservice.php?form=2";
        client=HttpClient.newBuilder().connectTimeout(connect).version(HttpClient.Version.HTTP_1_1).build();
    }
    public Verification verify(String transactionId){
        if(transactionId==null||transactionId.isBlank())throw new IllegalArgumentException("PayU transaction ID is required");
        String form="key="+encode(key)+"&command=verify_payment&var1="+encode(transactionId)+"&hash="+encode(hashes.commandHash("verify_payment",transactionId));
        HttpRequest request=HttpRequest.newBuilder(URI.create(endpoint)).timeout(readTimeout).header("Content-Type","application/x-www-form-urlencoded").header("Accept","application/json").POST(HttpRequest.BodyPublishers.ofString(form,StandardCharsets.UTF_8)).build();
        JsonNode root;
        try{
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("PayU verification returned HTTP "+response.statusCode());
            root=json.readTree(response.body());
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("PayU verification was interrupted",e);}catch(Exception e){throw new IllegalStateException("PayU verification request failed",e);}
        JsonNode detail=root==null?null:root.path("transaction_details").path(transactionId);
        if(detail==null||detail.isMissingNode()||"Not Found".equalsIgnoreCase(text(detail,"status")))return Verification.notFound(transactionId);
        if(!transactionId.equals(text(detail,"txnid")))throw new IllegalStateException("PayU returned a mismatched transaction ID");
        return new Verification(text(detail,"txnid"),text(detail,"status"),text(detail,"unmappedstatus"),decimal(detail,"amt"),text(detail,"mihpayid"),text(detail,"mode"),text(detail,"bank_ref_num"),text(detail,"error_Message"));
    }
    private static String text(JsonNode n,String field){JsonNode value=n.get(field);return value==null||value.isNull()?null:value.asText();}
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static BigDecimal decimal(JsonNode n,String field){String value=text(n,field);try{return value==null?null:new BigDecimal(value);}catch(NumberFormatException e){return null;}}
    public record Verification(String transactionId,String status,String unmappedStatus,BigDecimal amount,String payuPaymentId,String paymentMethod,String bankReference,String message){
        static Verification notFound(String id){return new Verification(id,"not_found","not_found",null,null,null,null,"Transaction not found at PayU");}
        public boolean captured(){return "success".equalsIgnoreCase(status)&&"captured".equalsIgnoreCase(unmappedStatus);}
        public boolean pending(){return "pending".equalsIgnoreCase(status)||"pending".equalsIgnoreCase(unmappedStatus)||"in progress".equalsIgnoreCase(unmappedStatus);}
        public boolean cancelled(){return unmappedStatus!=null&&unmappedStatus.toLowerCase().contains("cancel");}
    }
}
