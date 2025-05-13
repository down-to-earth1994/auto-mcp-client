package com.pig4cloud.pig.mcp.client.resource;

import java.util.List;

public interface ResourceManagerService {
    <T> List<T> loadJson(Class<T> clazz);
}
