package com.pig4cloud.pig.mcp.common.exception;

import com.pig4cloud.pig.mcp.common.util.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义异常处理
 */
public class RestException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(RestException.class.getCanonicalName());
    private final transient R r;

    public RestException(R r) {
        this.r = r;
        logger.error("RestException: {}", r, this);
    }
    public RestException(R r,Throwable arg0) {
        this.r = r;
    }
    public R getRestResult() {
        return this.r;
    }
}