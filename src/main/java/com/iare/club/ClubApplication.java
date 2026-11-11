package com.iare.club;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class ClubApplication {
	@Bean Clock utcClock() { return Clock.systemUTC(); }

	public static void main(String[] args) {
		SpringApplication.run(ClubApplication.class, args);
	}

}
