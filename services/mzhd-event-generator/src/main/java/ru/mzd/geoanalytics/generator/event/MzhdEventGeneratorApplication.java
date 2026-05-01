package ru.mzd.geoanalytics.generator.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "ru.mzd.geoanalytics.generator")
@EnableScheduling
@ConfigurationPropertiesScan
public class MzhdEventGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MzhdEventGeneratorApplication.class, args);
    }
}
