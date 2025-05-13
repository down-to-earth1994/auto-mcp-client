package com.pig4cloud.pig.mcp.common.bean;

import java.util.ArrayList;
import java.util.List;

public class RestResultCode {

    public static RestResultCode REST_COMMON_SUCCESS = new RestResultCode(10000, "REST_COMMON_SUCCESS", "success");
    public static RestResultCode REST_COMMON_FAILURE = new RestResultCode(10001, "REST_COMMON_FAILURE", "server failure");
    public static RestResultCode REST_COMMON_NOT_PERMITTED = new RestResultCode(10002, "REST_COMMON_NOT_PERMITTED", "not permitted");
    public static RestResultCode REST_COMMON_NOT_SUPPORT = new RestResultCode(10003, "REST_COMMON_NOT_SUPPORT", "not support request");
    public static RestResultCode REST_COMMON_FILE_SERVER_FAILURE = new RestResultCode(10004, "REST_COMMON_FILE_SERVER_FAILURE", "file server failure");
    public static RestResultCode REST_COMMON_AICP_SERVER_FAILURE = new RestResultCode(10005, "REST_COMMON_AICP_SERVER_FAILURE", "aicp server failure");
    public static RestResultCode REST_COMMON_INVALID_PARAMETER = new RestResultCode(10006, "REST_COMMON_INVALID_PARAMETER", "invalid parameter");
    public static RestResultCode REST_COMMON_NOT_MODIFIED = new RestResultCode(10007, "REST_COMMON_NOT_MODIFIED", "object not modified");
    public static RestResultCode REST_COMMON_USER_NO_LOGIN = new RestResultCode(10008, "REST_COMMON_USER_NO_LOGIN", "user no login");
    public static RestResultCode REST_COMMON_RATE_LIMIT = new RestResultCode(10009, "REST_COMMON_RATE_LIMIT", "api call rate exceeds limit");
    public static RestResultCode REST_COMMON_EXISTS = new RestResultCode(10010, "REST_COMMON_EXISTS", "object exits");
    public static RestResultCode REST_COMMON_NOT_FOUND = new RestResultCode(10011, "REST_COMMON_NOT_FOUND", "object not found");
    public static RestResultCode REST_COMMON_LOGIN_FAILURE = new RestResultCode(10012, "REST_COMMON_LOGIN_FAILURE", "login failure");
    public static RestResultCode REST_COMMON_INVALID_LANG_PARAMETER = new RestResultCode(10013, "REST_COMMON_INVALID_LANG_PARAMETER", "invalid lang parameter");
    public static RestResultCode REST_COMMON_NO_VALID_VERSION = new RestResultCode(10014, "REST_COMMON_NO_VALID_VERSION", "No valid version");
    public static RestResultCode REST_COMMON_EXISTING_EFFECTIVE_VERSION = new RestResultCode(10015, "REST_COMMON_EXISTING_EFFECTIVE_VERSION", "Rest common existing effective version");
    public static RestResultCode REST_COMMON_FILE_NOT_FOUND = new RestResultCode(10016, "REST_COMMON_FILE_NOT_FOUND", "file not exist");
    public static RestResultCode REST_COMMON_RESOURCE_NOT_FOUND = new RestResultCode(10017, "REST_COMMON_RESOURCE_NOT_FOUND", "resource info not exist");
    public static RestResultCode REST_COMMON_OBJ_USING_CANNOT_DELETED = new RestResultCode(10018, "REST_COMMON_OBJ_USING_CANNOT_DELETED", "Object is using, can't deleted");
    public static RestResultCode REST_COMMON_INVALID_IMPORT_TEMPLATE = new RestResultCode(10019, "REST_COMMON_INVALID_IMPORT_TEMPLATE", "invalid import template");
    public static RestResultCode REST_COMMON_CALL_OUT_OF_LIMIT = new RestResultCode(10020, "REST_COMMON_CALL_OUT_OF_LIMIT", "Interface call out of limit");
    public static RestResultCode REST_COMMON_OBJ_USING_CANNOT_UPDATE = new RestResultCode(10021, "REST_COMMON_OBJ_USING_CANNOT_UPDATE", "Object is using, can't update");

    public static final RestResultCode REST_CHECK_SIGNATURE_FAILED= new RestResultCode(10022, "验证签名错误", "验证签名错误");
    public static final RestResultCode REST_EXCEPTION_JDBC= new RestResultCode(10023, "数据库服务异常", "数据库服务异常");
    public static final RestResultCode REST_EXCEPTION_ES= new RestResultCode(10024, "elasticsearch服务异常", "elasticsearch服务异常");
    public static final RestResultCode REST_EXCEPTION_REDIS= new RestResultCode(10025, "redis服务异常", "redis服务异常");
    public static final RestResultCode REST_EXCEPTION_REQUEST= new RestResultCode(10026, "系统请求异常", "系统请求异常");

    int code;
    String name;
    String msg;

    protected static List<RestResultCode> resultCodes = new ArrayList<>();

    static {
        resultCodes.add(REST_COMMON_SUCCESS);
        resultCodes.add(REST_COMMON_FAILURE);
        resultCodes.add(REST_COMMON_NOT_PERMITTED);
        resultCodes.add(REST_COMMON_NOT_SUPPORT);
        resultCodes.add(REST_COMMON_FILE_SERVER_FAILURE);
        resultCodes.add(REST_COMMON_AICP_SERVER_FAILURE);
        resultCodes.add(REST_COMMON_INVALID_PARAMETER);
        resultCodes.add(REST_COMMON_NOT_MODIFIED);
        resultCodes.add(REST_COMMON_USER_NO_LOGIN);
        resultCodes.add(REST_COMMON_RATE_LIMIT);
        resultCodes.add(REST_COMMON_EXISTS);
        resultCodes.add(REST_COMMON_NOT_FOUND);
        resultCodes.add(REST_COMMON_LOGIN_FAILURE);
        resultCodes.add(REST_COMMON_INVALID_LANG_PARAMETER);
        resultCodes.add(REST_COMMON_NO_VALID_VERSION);
        resultCodes.add(REST_COMMON_EXISTING_EFFECTIVE_VERSION);
        resultCodes.add(REST_COMMON_FILE_NOT_FOUND);
        resultCodes.add(REST_COMMON_RESOURCE_NOT_FOUND);
        resultCodes.add(REST_COMMON_OBJ_USING_CANNOT_DELETED);
        resultCodes.add(REST_COMMON_CALL_OUT_OF_LIMIT);
        resultCodes.add(REST_COMMON_INVALID_IMPORT_TEMPLATE);
        resultCodes.add(REST_COMMON_OBJ_USING_CANNOT_UPDATE);
        resultCodes.add(REST_CHECK_SIGNATURE_FAILED);
        resultCodes.add(REST_EXCEPTION_JDBC);
        resultCodes.add(REST_EXCEPTION_ES);
        resultCodes.add(REST_EXCEPTION_REDIS);
        resultCodes.add(REST_EXCEPTION_REQUEST);
    }

    public static List<RestResultCode> values() {
        return resultCodes;
    }

    public RestResultCode() {
    }

    public RestResultCode(int code, String name, String msg) {
        this.code = code;
        this.name = name;
        this.msg = msg;
    }

    public static String getMsg(int code) {
        for (RestResultCode r : RestResultCode.values()) {
            if (r.getCode() == code) {
                return r.getMsg();
            }
        }
        return null;
    }

    public static RestResultCode getResultCode(int code) {
        for (RestResultCode r : RestResultCode.values()) {
            if (r.getCode() == code) {
                return r;
            }
        }
        return RestResultCode.REST_COMMON_FAILURE;
    }

    public String nameLower() {
        return this.name.toLowerCase();
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }

    public boolean success() {
        return getCode() == REST_COMMON_SUCCESS.getCode();
    }
}
