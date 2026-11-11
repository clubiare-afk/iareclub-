package com.iare.club.service;
import java.math.BigDecimal;import java.security.SecureRandom;import org.springframework.stereotype.Component;
@Component public class ScratchRewardGenerator{
 private final SecureRandom random=new SecureRandom();
 public BigDecimal generate(){int ticket=random.nextInt(1_000_000);return rewardForTicket(ticket,random.nextInt(15),random.nextInt(5),random.nextInt(29));}
 public BigDecimal rewardForTicket(int ticket,int commonIndex,int uncommonIndex,int rareIndex){if(ticket<0||ticket>=1_000_000)throw new IllegalArgumentException("ticket out of range");if(ticket<500)return new BigDecimal("50.00");if(ticket<5_000)return BigDecimal.valueOf(21L+Math.floorMod(rareIndex,29)).setScale(2);if(ticket<50_000)return BigDecimal.valueOf(16L+Math.floorMod(uncommonIndex,5)).setScale(2);return BigDecimal.valueOf(1L+Math.floorMod(commonIndex,15)).setScale(2);}
}
