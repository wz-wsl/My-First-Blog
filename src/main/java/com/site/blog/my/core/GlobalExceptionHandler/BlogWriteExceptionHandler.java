package com.site.blog.my.core.GlobalExceptionHandler;

import com.site.blog.my.core.Exception.BlogWriteException;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BlogWriteExceptionHandler {
    // 异常到达 Controller 时，Service 事务已经回滚，保持前端原有的 Result 格式。
    @ExceptionHandler(BlogWriteException.class)
    public Result handleBlogWriteFailure(BlogWriteException exception) {
        return ResultGenerator.genFailResult(exception.getMessage());
    }
}
