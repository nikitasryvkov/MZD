package ru.mzd.geoanalytics.generator.event;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component("eventGeneratorProperties")
@ConfigurationProperties(prefix = "generator.event")
public class EventGeneratorProperties {

    @NotBlank
    private String sourceSystem = "mzhd-event-generator";

    @NotBlank
    private String timeZone = "Europe/Moscow";

    @NotNull
    private Duration tickInterval = Duration.ofSeconds(45);

    @Min(1)
    private int maxActiveEvents = 6;

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

    public int getMaxActiveEvents() {
        return maxActiveEvents;
    }

    public void setMaxActiveEvents(int maxActiveEvents) {
        this.maxActiveEvents = maxActiveEvents;
    }
}
