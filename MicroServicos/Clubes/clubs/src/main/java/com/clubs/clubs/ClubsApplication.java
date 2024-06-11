package com.clubs.clubs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableEurekaClient
public class ClubsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClubsApplication.class, args);
	}

}
