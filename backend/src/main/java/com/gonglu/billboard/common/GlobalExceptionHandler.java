package com.gonglu.billboard.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> biz(BizException e) {
        return R.error(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> denied(AccessDeniedException e) {
        return R.error(403, "无权限执行该操作");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> invalid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        return R.error(fe != null ? fe.getField() + " " + fe.getDefaultMessage() : "参数校验失败");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public R<Void> badRequest(Exception e) {
        return R.error("请求参数格式错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> error(Exception e) {
        log.error("系统异常", e);
        return R.error(500, "系统繁忙，请稍后再试");
    }
}
