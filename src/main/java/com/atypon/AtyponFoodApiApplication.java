package com.atypon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration.class
})
@ConfigurationPropertiesScan(basePackages = "com.atypon")
public class AtyponFoodApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtyponFoodApiApplication.class, args);
	}

}
