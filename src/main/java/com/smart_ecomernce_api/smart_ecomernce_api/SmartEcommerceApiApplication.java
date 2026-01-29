package com.smart_ecomernce_api.smart_ecomernce_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.data.jpa.repository.config.EnableJpaAuditing;



@SpringBootApplication
@EnableJpaAuditing
public class SmartEcommerceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartEcommerceApiApplication.class, args);
	}

}
