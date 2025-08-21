package com.example.tplspringboot;

import org.springframework.boot.SpringApplication;

public class TestTplSpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.from(TplSpringBootApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
