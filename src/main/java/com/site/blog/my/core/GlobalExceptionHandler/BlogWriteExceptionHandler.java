package com.site.blog.my.core.GlobalExceptionHandler;

import com.site.blog.my.core.Exception.BlogWriteException;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

@Slf4j
@RestControllerAdvice
public class BlogWriteExceptionHandler {
    // 异常到达 Controller 时，Service 事务已经回滚，保持前端原有的 Result 格式。
    @ExceptionHandler(BlogWriteException.class)
    public Result handleBlogWriteFailure(BlogWriteException exception) {
        return ResultGenerator.genFailResult(exception.getMessage());
    }

    // SQL 异常同样已经回滚；细节只写日志，不返回给前端。
    // 只接管返回 JSON 的接口，页面请求原样抛出，仍走错误页。
    @ExceptionHandler(DataAccessException.class)
    public Result handleDataAccessFailure(DataAccessException exception, HandlerMethod handlerMethod) throws DataAccessException {
        boolean jsonEndpoint = handlerMethod.hasMethodAnnotation(ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), ResponseBody.class);
        if (!jsonEndpoint) {
            throw exception;
        }
        log.error("数据库写入失败", exception);
        return ResultGenerator.genFailResult("数据保存失败，请稍后重试");
    }
}
