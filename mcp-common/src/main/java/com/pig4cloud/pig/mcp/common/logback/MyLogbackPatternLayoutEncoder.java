package com.pig4cloud.pig.mcp.common.logback;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

/**
 * @Author zhangtengfei
 * @Date 2022/12/23 11:12
 */
public class MyLogbackPatternLayoutEncoder extends PatternLayoutEncoder {
    /**
     * 正则替换规则
     */
    private MyLogbackReplaces replaces;
    /**
     * 是否开启脱敏，默认关闭(false）
     */
    private Boolean sensitive = false;

    /**
     * 使用自定义 MyLogbackPatternLayout 格式化输出
     */
    @Override
    public void start() {
        MyLogbackPatternLayout patternLayout = new MyLogbackPatternLayout(replaces, sensitive);
        patternLayout.setContext(context);
        patternLayout.setPattern(this.getPattern());
        patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
        patternLayout.start();
        this.layout = patternLayout;
        started = true;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public MyLogbackReplaces getReplaces() {
        return replaces;
    }

    public void setReplaces(MyLogbackReplaces replaces) {
        this.replaces = replaces;
    }
}
