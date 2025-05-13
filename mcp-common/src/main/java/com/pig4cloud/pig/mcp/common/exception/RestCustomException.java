package com.pig4cloud.pig.mcp.common.exception;


import com.pig4cloud.pig.mcp.common.util.R;

import lombok.extern.slf4j.Slf4j;

/**
 * 自定义rest异常处理
 */
@Slf4j
public class RestCustomException extends RuntimeException {
    private final transient R r;

    public RestCustomException(R r) {
        this.r = r;
        log.warn("RestCustomException: {}", r, this);
    }

    public R getRestResult() {
        return this.r;
    }
}