package com.helpscout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages={"com.helpscout.controller", "com.helpscout.dao", "com.helpscout.service"})
public class Application {

    public static final void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

}

