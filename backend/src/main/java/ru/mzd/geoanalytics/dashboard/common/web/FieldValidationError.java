package ru.mzd.geoanalytics.dashboard.common.web;

public record FieldValidationError(
    String field,
    String message
) {
}
