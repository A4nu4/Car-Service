package com.carrental.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI carRentalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Car Rental API")
                        .version("v1")
                        .description("""
                                REST API for the car rental platform.

                                **Authentication:** call `POST /api/v1/auth/login` (or `/register`), then send the
                                `accessToken` as `Authorization: Bearer <token>`. Access tokens expire after 15 minutes;
                                exchange the `refreshToken` at `POST /api/v1/auth/refresh` for a new pair. Each refresh
                                token works once; reusing an old one logs the user out everywhere.

                                **Errors** use RFC 9457 Problem Details (`application/problem+json`) with a stable
                                machine-readable `code` and, for validation failures, an `errors` array of
                                `{ field, message }`.

                                **Timestamps** are ISO-8601 instants in UTC, e.g. `2026-11-20T09:00:00Z`.
                                **Money** is a decimal number with two fraction digits, in EUR.
                                **Lists** are paginated with `?page=0&size=20&sort=field,asc`.
                                """))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
