package com.snowwave.p2p;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class P2pServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(P2pServerApplication.class, args);
	}

}
