package com.site.blog.my.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分页查询参数
 */
public class PageQueryUtil extends LinkedHashMap<String, Object> {
    //当前页码
    private int page;
    //每页条数
    private int limit;

    /** 单页最多条数，防止一次查询过多数据 */
    private static final int MAX_LIMIT = 100;

    public PageQueryUtil(Map<String, Object> params) {
        this.putAll(params);

        //分页参数：页码至少为 1，条数限制在 1~100，非数字按默认值处理，避免 LIMIT 出现负数
        this.page = Math.max(1, parseInt(params.get("page"), 1));
        this.limit = Math.min(MAX_LIMIT, Math.max(1, parseInt(params.get("limit"), 10)));
        //页码过大时 (page - 1) * limit 会溢出成负数，这里先封顶
        this.page = Math.min(this.page, Integer.MAX_VALUE / limit);
        this.put("start", (page - 1) * limit);
        this.put("page", page);
        this.put("limit", limit);
    }


    private static int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "PageUtil{" +
                "page=" + page +
                ", limit=" + limit +
                '}';
    }
}
