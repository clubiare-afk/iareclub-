package com.iare.club.service;
import org.springframework.stereotype.Service;
import java.time.*;import java.util.concurrent.*;
@Service public class RateLimitService{
 private record Window(long minute,int count){} private final ConcurrentMap<String,Window> windows=new ConcurrentHashMap<>();
 public void check(String key,int max){long minute=Instant.now().getEpochSecond()/60;Window next=windows.compute(key,(k,w)->w==null||w.minute()!=minute?new Window(minute,1):new Window(minute,w.count()+1));if(next.count()>max)throw new RateLimitExceededException();}
 public static class RateLimitExceededException extends RuntimeException{}
}
