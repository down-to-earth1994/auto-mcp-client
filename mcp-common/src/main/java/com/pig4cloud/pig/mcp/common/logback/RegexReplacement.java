package com.pig4cloud.pig.mcp.common.logback;

import java.util.regex.Pattern;

/**
 * @Author zhangtengfei
 * @Date 2022/12/23 11:13
 */
public class RegexReplacement {
    /**
     * 脱敏匹配正则
     */
    private Pattern regex;

    /**
     * 替换正则
     */
    private String replacement;

    /**
     * Perform the replacement.
     *
     * @param msg The String to match against.
     * @return the replacement String.
     */
    public String format(final String msg) {
        return regex.matcher(msg).replaceAll(replacement);
    }

    public Pattern getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = Pattern.compile(regex);
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
