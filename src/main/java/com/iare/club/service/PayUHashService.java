package com.iare.club.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PayUHashService {
    private final String key;
    private final String salt;
    public PayUHashService(@Value("${payu.merchant-key:}")String key,@Value("${payu.salt:}")String salt){this.key=key.strip();this.salt=salt.strip();}
    public String paymentHash(String txnid,String amount,String product,String firstName,String email,String udf1){
        return sha512(key+"|"+txnid+"|"+amount+"|"+product+"|"+firstName+"|"+email+"|"+n(udf1)+"||||||||||"+salt);
    }
    public String commandHash(String command,String value){return sha512(String.join("|",key,command,value,salt));}
    public boolean validResponse(Map<String,String> p){
        if(!key.equals(p.get("key"))||p.get("hash")==null)return false;
        String base=salt+"|"+n(p.get("status"))+"||||||"+n(p.get("udf5"))+"|"+n(p.get("udf4"))+"|"+n(p.get("udf3"))+"|"+n(p.get("udf2"))+"|"+n(p.get("udf1"))+"|"+n(p.get("email"))+"|"+n(p.get("firstname"))+"|"+n(p.get("productinfo"))+"|"+n(p.get("amount"))+"|"+n(p.get("txnid"))+"|"+n(p.get("key"));
        String additional=p.get("additionalCharges");
        if(additional==null)additional=p.get("additional_charges");
        if(additional!=null&&!additional.isBlank())base=additional+"|"+base;
        return MessageDigest.isEqual(sha512(base).getBytes(StandardCharsets.US_ASCII),p.get("hash").getBytes(StandardCharsets.US_ASCII));
    }
    private String sha512(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static String n(String value){return value==null?"":value;}
}
