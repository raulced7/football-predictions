package com.raulcedroj.football_predictions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FootballPredictionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FootballPredictionsApplication.class, args);
	}

}
