package com.tlias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class TliasApplication {
    public static void main(String[] args) {
        SpringApplication.run(TliasApplication.class, args);
    }
}
