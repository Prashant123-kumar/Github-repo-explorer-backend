package com.prashant.githubexplorer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GitHub Repo Explorer API")
                        .version("1.0.0")
                        .description("""
                                Proxy backend for the GitHub Repo Explorer assignment.
                                
                                **Key features:**
                                - Proxies GitHub REST API v3
                                - In-memory Caffeine cache (60 s TTL)
                                - Rate-limit & error handling
                                - Sorting, pagination, and language statistics
                                """)
                        .contact(new Contact()
                                .name("Prashant Kumar")
                                .email("kumarprashantshivam456@gmail.com"))
                        .license(new License()
                                .name("MIT")))
                .servers(List.of(
                        new Server()
                                .url("https://github-repo-explorer-api-8sgt.onrender.com")
                                .description("Production Server")
                ));
    }
}