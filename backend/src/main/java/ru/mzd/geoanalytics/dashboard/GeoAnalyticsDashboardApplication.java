package ru.mzd.geoanalytics.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class GeoAnalyticsDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeoAnalyticsDashboardApplication.class, args);
    }
}
