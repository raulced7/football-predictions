package com.raulcedroj.football_predictions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FootballPredictionsApplicationTests {

	@Test
	void contextLoads() {
	}

}
