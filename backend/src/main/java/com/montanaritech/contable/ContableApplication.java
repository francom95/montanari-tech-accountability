package com.montanaritech.contable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @EnableScheduling} (F9.1): primer job programado del proyecto, el motor de alertas diario. */
@SpringBootApplication
@EnableScheduling
public class ContableApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContableApplication.class, args);
    }
}
