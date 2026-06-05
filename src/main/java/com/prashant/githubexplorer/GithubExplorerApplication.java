package com.prashant.githubexplorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GithubExplorerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubExplorerApplication.class, args);
    }
}