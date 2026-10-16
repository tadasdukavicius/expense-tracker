package com.example.expensetracker;

import org.springframework.boot.SpringApplication;

public class TestExpensetrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(ExpensetrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
