package ru.mzd.geoanalytics.generator.personnel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "ru.mzd.geoanalytics.generator")
@EnableScheduling
@ConfigurationPropertiesScan
public class MzhdPersonnelGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MzhdPersonnelGeneratorApplication.class, args);
    }
}
