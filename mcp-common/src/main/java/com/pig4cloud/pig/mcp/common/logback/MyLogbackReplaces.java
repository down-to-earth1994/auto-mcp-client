package com.pig4cloud.pig.mcp.common.logback;



import java.util.ArrayList;
import java.util.List;

/**
 * @Author zhangtengfei
 * @Date 2022/12/23 11:13
 */
public class MyLogbackReplaces {
    /**
     * 脱敏正则列表
     */
    private List<RegexReplacement> replace = new ArrayList<>();

    /**
     * 添加规则（因为replace类型是list，必须指定addReplace方法用以添加多个）
     *
     * @param replacement replacement
     */
    public void addReplace(RegexReplacement replacement) {
        replace.add(replacement);
    }

    public List<RegexReplacement> getReplace() {
        return replace;
    }

    public void setReplace(List<RegexReplacement> replace) {
        this.replace = replace;
    }
}
