package com.pig4cloud.pig.mcp.common.util;


import com.google.common.collect.Maps;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpSseUrlUtils {
    public static final String DEFAULT_BASE_URL_NAME = "baseUrl";
    public static final String DEFAULT_ENDPOINT_NAME = "endpoint";
    public static final String DEFAULT_ENDPOINT_VALUE = "/sse";


    /**
     * 切割url 为baseurl 和 sse 断点
     *
     * @param url 原始 URL
     * @return 切割后的 URL，如果 URL 不存在则返回原始 URL
     */
    public static Map<String, String> splitUrl(String url) {
        Map<String, String> result = Maps.newHashMap();
        int index = url.indexOf(DEFAULT_ENDPOINT_VALUE);
        if (index != -1) {
            String baseUrl = url.substring(0, index);
            String endpoint = url.substring(index);
            result.put(DEFAULT_BASE_URL_NAME, baseUrl);
            result.put(DEFAULT_ENDPOINT_NAME, endpoint);
        } else {
            result.put(DEFAULT_BASE_URL_NAME, url);
            result.put(DEFAULT_ENDPOINT_NAME, DEFAULT_ENDPOINT_VALUE);
        }
        return result;
    }

    /**
     * 检查 URL 是否存在。
     *
     * @param url URL 地址
     * @return true 如果 URL 存在，false 否则
     */
    private static boolean urlExists(String url) {
        HttpURLConnection huc = null;
        try {
            URL u = new URL(url);
            huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("GET");
            // 设置超时时间
            huc.setConnectTimeout(5000); // 连接超时时间为 5 秒
            huc.setReadTimeout(5000);    // 读取超时时间为 5 秒
            int responseCode = huc.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            log.error("【HTTP-SSE】 URL检查:{} GET请求返回不是200", url, e);
            return false;
        } finally {
            if (huc != null) {
                huc.disconnect();
            }
        }
    }

    public static void main(String[] args) {
        String url1 = "https://mcp.amap.com/sse?key=79b3a68afd8cd4d81341e6c7aa41eddf";
        String url2 = "https://mcp.amap.com";
        Map<String, String> result = splitUrl(url1);

        Map<String, String> result2 = splitUrl(url2);
        System.out.println("处理后的 URL 1: " + result.get(DEFAULT_BASE_URL_NAME) + ":" + result.get(DEFAULT_ENDPOINT_NAME));
        System.out.println("处理后的 URL 2: " + result2.get(DEFAULT_BASE_URL_NAME) + ":" + result2.get(DEFAULT_ENDPOINT_NAME));
    }
}