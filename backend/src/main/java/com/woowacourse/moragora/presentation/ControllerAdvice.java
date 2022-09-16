package com.woowacourse.moragora.presentation;

import com.woowacourse.moragora.dto.response.ErrorResponse;
import com.woowacourse.moragora.exception.ClientRuntimeException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(ClientRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleClientRuntimeException(final ClientRuntimeException exception) {
        return ResponseEntity.status(exception.getHttpStatus()).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(final BindingResult bindingResult) {
        final List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        final FieldError mainError = fieldErrors.get(0);

        return ResponseEntity.badRequest().body(new ErrorResponse(mainError.getDefaultMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestFormat() {
        return ResponseEntity.badRequest().body(new ErrorResponse("입력 형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerError(final Exception exception) {
        log.error("Internal Server Error\n{}", exception.getMessage(), exception);
        return ResponseEntity.internalServerError().body(new ErrorResponse("서버에 오류가 발생했습니다."));
    }
}
