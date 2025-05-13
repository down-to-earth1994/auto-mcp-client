package com.pig4cloud.pig.mcp.client.resource;


import com.pig4cloud.pig.mcp.common.bean.RestResultCode;
import com.pig4cloud.pig.mcp.common.exception.RestCustomException;
import com.pig4cloud.pig.mcp.common.properties.ExternalConfigProperties;
import com.pig4cloud.pig.mcp.common.util.JsonUtil;
import com.pig4cloud.pig.mcp.common.util.R;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResourceManagerServiceImpl implements ResourceManagerService {
    private final ResourceLoader resourceLoader;
    private final ExternalConfigProperties externalConfigProperties;

    public ResourceManagerServiceImpl(ResourceLoader resourceLoader, ExternalConfigProperties externalConfigProperties) {
        this.resourceLoader = resourceLoader;
        this.externalConfigProperties = externalConfigProperties;
    }

    public <T> List<T> loadJson(Class<T> clazz) {
        try {
            return doLoadJson(clazz);
        } catch (Exception e) {
            log.error("解析默认MCP配置失败", e);
            throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_FILE_NOT_FOUND.getCode(), "读取默认MCP SERVER CONFIG 失败"));
        }
    }

    private <T> List<T> doLoadJson(Class<T> clazz) throws IOException {
        String filePath = externalConfigProperties.getJsonFilePath();
        if (filePath != null && !filePath.isEmpty()) {
            // 如果配置了外部文件路径，则优先读取外部文件
            return JsonUtil.readJson("file:" + filePath, clazz, resourceLoader);
        } else {
            // 否则读取 resources 下的默认文件
            return JsonUtil.readJson("classPath:default-mcp-registry.json", clazz, resourceLoader);
        }
    }


}
