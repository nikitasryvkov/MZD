package ru.mzd.geoanalytics.generator.train;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component("trainGeneratorProperties")
@ConfigurationProperties(prefix = "generator.train")
public class TrainGeneratorProperties {

    @NotBlank
    private String sourceSystem = "mzhd-train-generator";

    @NotBlank
    private String timeZone = "Europe/Moscow";

    @NotNull
    private Duration tickInterval = Duration.ofSeconds(15);

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Duration getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(Duration tickInterval) {
        this.tickInterval = tickInterval;
    }
}
