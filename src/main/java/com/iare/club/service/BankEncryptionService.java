package com.iare.club.service;
import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;
import javax.crypto.*;import javax.crypto.spec.*;import java.nio.charset.StandardCharsets;import java.security.SecureRandom;import java.util.*;
@Service public class BankEncryptionService{
 private final SecretKey key;private final SecureRandom random=new SecureRandom();
 public BankEncryptionService(@Value("${moneymint.bank-encryption-key}")String encoded){byte[] bytes=Base64.getDecoder().decode(encoded);if(bytes.length!=32)throw new IllegalStateException("BANK_ENCRYPTION_KEY must decode to exactly 32 bytes");key=new SecretKeySpec(bytes,"AES");}
 public String encrypt(String value){try{byte[] iv=new byte[12];random.nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));byte[] encrypted=c.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] combined=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,combined,0,iv.length);System.arraycopy(encrypted,0,combined,iv.length,encrypted.length);return Base64.getEncoder().encodeToString(combined);}catch(Exception e){throw new IllegalStateException("Unable to protect bank data",e);}}
 public String decrypt(String value){try{byte[] all=Base64.getDecoder().decode(value),iv=Arrays.copyOfRange(all,0,12),encrypted=Arrays.copyOfRange(all,12,all.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));return new String(c.doFinal(encrypted),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Unable to read protected bank data",e);}}
}
