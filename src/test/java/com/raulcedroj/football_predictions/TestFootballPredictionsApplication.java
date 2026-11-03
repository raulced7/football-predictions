package com.raulcedroj.football_predictions;

import org.springframework.boot.SpringApplication;

public class TestFootballPredictionsApplication {

	public static void main(String[] args) {
		SpringApplication.from(FootballPredictionsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
