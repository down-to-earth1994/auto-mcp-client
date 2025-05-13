package com.pig4cloud.pig.mcp.common.advice;


import com.pig4cloud.pig.mcp.common.bean.RestResultCode;
import com.pig4cloud.pig.mcp.common.exception.RestCustomException;
import com.pig4cloud.pig.mcp.common.exception.RestException;
import com.pig4cloud.pig.mcp.common.util.R;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.UnexpectedTypeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@RestControllerAdvice
public class MvcGlobalExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(MvcGlobalExceptionAdvice.class);


    @ExceptionHandler(value = {BindException.class, UnexpectedTypeException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public R catchParamsValidateException(Exception exception) {
        logger.warn("ParamsValidateException map {} to response, message:{}", exception.getCause(), exception.getMessage(), exception);
        if (exception instanceof ConstraintViolationException) {
            Set<ConstraintViolation<?>> constraintViolations = ((ConstraintViolationException) exception).getConstraintViolations();
            List<String> errors = new ArrayList<>();
            for (ConstraintViolation<?> violation : constraintViolations) {
                errors.add(violation.getMessage());
            }
            return R.failed(exception.getMessage());
        }
        List<String> errors = new ArrayList<>();
        if (exception instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) exception;
            BindingResult bindResult = e.getBindingResult();
            //TODO 通过反射查找注解 获取中文field名称
            for (FieldError error : bindResult.getFieldErrors()) {
                errors.add(/*error.getField() + " " +*/ error.getDefaultMessage());
            }
        } else if (exception instanceof BindException) {
            BindingResult bindResult = ((BindException) exception).getBindingResult();
            for (ObjectError error : bindResult.getAllErrors()) {
                errors.add(error.getDefaultMessage());
            }
        }
        return R.failed(exception.getMessage());
    }

    @ExceptionHandler(RestException.class)
    public R catchRestException(RestException exception) {
        logger.warn("RestException message:{}", exception.getRestResult());
        R restResult = exception.getRestResult();
        if (restResult != null && StringUtils.isEmpty(restResult.getMsg())) {
            restResult.setMsg(RestResultCode.getResultCode(restResult.getCode()).nameLower());
        } else {
            restResult.setData(restResult.getMsg());
            restResult.setMsg(RestResultCode.getResultCode(restResult.getCode()).nameLower());
        }
        return restResult;
    }

    //与 RestException 不同的地方就是 RestException无论如何都会根据code 去重置msg，而RestCustomException会返回自定义的msg
    @ExceptionHandler(RestCustomException.class)
    public R catchRestCustomException(RestCustomException exception) {
        logger.warn("RestCustomException message:{}", exception.getRestResult());
        R restResult = exception.getRestResult();
        if (restResult == null) {
            return restResult;
        }
        if (StringUtils.isEmpty(restResult.getMsg())) {
            restResult.setMsg(RestResultCode.getResultCode(restResult.getCode()).nameLower());
            return restResult;
        }
        if (restResult.getData()==null) {
            restResult.setData(restResult.getMsg());
        }
        return restResult;
    }

    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    public R catchClientBadRequestException(Exception exception) {
        logger.warn("BadRequest map {} to reponse, message:{}", exception.getCause(), exception.getMessage());
        return R.generic(RestResultCode.REST_COMMON_NOT_SUPPORT.getCode(),RestResultCode.REST_COMMON_NOT_SUPPORT.nameLower());
    }
}
