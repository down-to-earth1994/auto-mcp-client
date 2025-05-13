//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.pig4cloud.pig.mcp.common.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pig4cloud.pig.mcp.common.exception.JsonException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public JsonUtil() {
    }

    public void writeTo(OutputStream out) {
        writeTo(out, this);
    }

    public String toString() {
        return encodeToString(this);
    }

    public static final String encodeToString(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static final String encode(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static final <T> List<T> decodeList(String json, Class<T> obj) {
        try {
            JavaType javaType = getCollectionType(ArrayList.class, obj);
            return (List) MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static final <T> T decode(String json, Class<T> obj) {
        try {
            return (T) MAPPER.readValue(json, obj);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static final <T> T decodeFromString(String value, Class<T> cls) {
        try {
            return (T) MAPPER.readValue(value, cls);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static final void writeTo(OutputStream out, Object value) {
        try {
            MAPPER.writeValue(out, value);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public static final <T> T jsonToBeanByRef(String json, TypeReference<T> ref) throws Exception {
        try {
            return (T) MAPPER.readValue(json, ref);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public static JavaType getCollectionType(Class<?> collCls, Class<?>... elCls) {
        return MAPPER.getTypeFactory().constructParametricType(collCls, elCls);
    }

    public static <T> List<T> readJson(String location, Class<T> clazz, ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            JavaType javaType = getCollectionType(ArrayList.class, clazz);
            return (List)MAPPER.readValue(inputStream, javaType);
        }
    }

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.setSerializationInclusion(Include.NON_NULL);
        MAPPER.setDateFormat(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
    }
}
