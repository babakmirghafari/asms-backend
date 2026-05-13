package com.asms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ASMS Backend Application entry point.
 *
 * <p>This class MUST remain in the root package {@code com.asms} so that
 * Spring's component scan discovers all beans in:
 * <ul>
 *   <li>{@code com.asms.*} — service delegates, repositories, domain, mappers, config</li>
 *   <li>{@code com.asms.api.*} — generated API controllers (shipped in the contract JAR)</li>
 * </ul>
 */
@SpringBootApplication
public class AsmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsmsApplication.class, args);
    }
}
