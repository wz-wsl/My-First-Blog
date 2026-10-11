package com.site.blog.my.core.Exception;

/** 博客业务校验或数据库写入失败时抛出，让相关事务整体回滚。 */
public class BlogWriteException extends RuntimeException {
    public BlogWriteException(String message) {
        super(message);
    }
}
