package com.dave.ai.bi.helper.controller;

import com.dave.common.domain.vo.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return R.fail(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public R<Void> handleIllegalState(IllegalStateException ex) {
        return R.fail(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleGenericException(Exception ex) {
        log.error("Unhandled request failure", ex);
        return R.fail("系统处理失败: " + ex.getMessage());
    }
}
