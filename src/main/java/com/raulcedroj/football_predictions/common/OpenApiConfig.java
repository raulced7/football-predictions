package com.raulcedroj.football_predictions.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI footballPredictionsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Football Predictions API")
                        .version("1.0.0")
                        .description("Football prediction games between friends: groups, predictions, "
                                + "automatic scoring and standings.")
                        .contact(new Contact()
                                .name("Raúl Cedeño Rojo")
                                .url("https://www.linkedin.com/in/raulcedroj/")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}