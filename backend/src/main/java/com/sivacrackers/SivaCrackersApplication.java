package com.sivacrackers;

import com.sivacrackers.config.RequiredSettingsCheck;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * SIVA Crackers REST API.
 *
 * The public website is an information catalogue only: this application has no
 * checkout, online payment, online ordering, booking or reservation features.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SivaCrackersApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SivaCrackersApplication.class);
        app.addListeners(new RequiredSettingsCheck()); // fail fast with a clear list of missing settings
        app.run(args);
    }
}
