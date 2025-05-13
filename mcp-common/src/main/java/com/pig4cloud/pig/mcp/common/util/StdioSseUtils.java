package com.pig4cloud.pig.mcp.common.util;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StdioSseUtils {
    /**
     * 检查字符串数组中是否包含以 "@" 开头的字符串。
     *
     * @param args 字符串数组
     * @return 第一个以 "@" 开头的字符串，如果没有找到则返回 null。
     */
    public static String findStdioClientName(String[] args) {
        if (args == null || args.length == 0) {
            return null; // 数组为空或 null，直接返回 null
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith("@")) {
                return arg; // 找到以 "@" 开头的字符串，立即返回
            }
        }
        return null; // 没有找到任何以 "@" 开头的字符串，返回 null
    }

    /**
     * 将 List<String> 转换为 String 数组。
     * 如果 List 为 null 或为空，则返回一个空数组 (String[0])。
     *
     * @param list 要转换的 List<String>。
     * @return 转换后的 String 数组，如果 List 为 null 或为空，则返回空数组。
     */
    public static String[] convertListToArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return new String[0]; // 返回一个空数组
        } else {
            return list.toArray(new String[0]); // 使用 toArray() 方法转换为数组
        }
    }
    public static void main(String[] args) {
        // 示例用法
        String[] myArgs = { "-y", "@modelcontextprotocol/server-filesystem", "C:\\Users\\heyanfeng\\Desktop", "C:\\Users\\heyanfeng\\Downloads", "C:\\Users\\heyanfeng\\Desktop\\markdown\\work" };
        String atArg = StdioSseUtils.findStdioClientName(myArgs);
        if (atArg != null) {
            System.out.println("找到以 @ 开头的参数: " + atArg);
        } else {
            System.out.println("未找到以 @ 开头的参数。");
        }
        String[] emptyArgs = {};
        String atArg2 = StdioSseUtils.findStdioClientName(emptyArgs);
        System.out.println("atArg2 is null: " + (atArg2 == null));
    }
}
