package com.example.rental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI rentalMobilOpenAPI() {
        return new OpenAPI().info(new Info()
            .title("Rental Mobil API")
            .version("v1")
            .description("REST API untuk master data mobil dan transaksi rental."));
    }
}
