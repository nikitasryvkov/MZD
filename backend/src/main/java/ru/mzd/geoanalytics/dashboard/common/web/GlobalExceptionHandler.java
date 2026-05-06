package ru.mzd.geoanalytics.dashboard.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.mzd.geoanalytics.dashboard.common.exception.ConflictException;
import ru.mzd.geoanalytics.dashboard.common.exception.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldValidationError)
            .toList();

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
            "validation_error",
            "Ошибка валидации запроса.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            fieldErrors,
            null,
            null,
            null
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getConstraintViolations()
            .stream()
            .map(violation -> new FieldValidationError(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            ))
            .toList();

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
            "validation_error",
            "Ошибка валидации запроса.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            fieldErrors,
            null,
            null,
            null
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
            "validation_error",
            "Некорректное тело запроса.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            List.of(),
            null,
            null,
            null
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
            "validation_error",
            "Некорректный параметр запроса.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            List.of(new FieldValidationError(exception.getName(), "invalid value")),
            null,
            null,
            null
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
        ResourceNotFoundException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
            "not_found",
            exception.getMessage(),
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            null,
            null,
            null,
            null
        ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
        ConflictException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
            "conflict",
            exception.getMessage(),
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            null,
            exception.getCurrentStatus(),
            exception.getRequestedStatus(),
            exception.getAllowedTransitions()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(
            "forbidden",
            exception.getMessage(),
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            null,
            null,
            null,
            null
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        log.warn(
            "Нарушение ограничения данных при обработке {} {}.",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
            "data_integrity_error",
            "Запрос нарушает ограничения данных.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            null,
            null,
            null,
            null
        ));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnected(
        AsyncRequestNotUsableException exception,
        HttpServletRequest request
    ) {
        log.debug(
            "РљР»РёРµРЅС‚ Р·Р°РєСЂС‹Р» СЃРѕРµРґРёРЅРµРЅРёРµ РґРѕ Р·Р°РІРµСЂС€РµРЅРёСЏ РѕС‚РІРµС‚Р° {} {}.",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error(
            "Необработанное исключение при обработке {} {}.",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
            "server_error",
            "Произошла непредвиденная ошибка.",
            RequestContext.requestId(request),
            RequestContext.traceId(request),
            null,
            null,
            null,
            null
        ));
    }

    private FieldValidationError toFieldValidationError(FieldError fieldError) {
        return new FieldValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
