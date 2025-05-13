package com.pig4cloud.pig.mcp.common.exception;

public class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String arg0) {
        super(arg0);
    }

    public BaseException(Throwable arg0) {
        super(arg0);
    }

    public BaseException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
