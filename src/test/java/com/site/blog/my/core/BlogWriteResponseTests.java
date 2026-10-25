package com.site.blog.my.core;

import com.site.blog.my.core.controller.admin.BlogController;
import com.site.blog.my.core.GlobalExceptionHandler.BlogWriteExceptionHandler;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.Exception.BlogWriteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BlogWriteResponseTests {
    @ParameterizedTest
    @ValueSource(strings = {"save", "update"})
    void writeFailureKeepsAdminJsonContract(String action) throws Exception {
        BlogService service = mock(BlogService.class);
        when(service.saveBlog(any())).thenThrow(new BlogWriteException("文章标签关系保存不完整"));
        when(service.updateBlog(any())).thenThrow(new BlogWriteException("文章标签关系保存不完整"));
        BlogController controller = new BlogController();
        ReflectionTestUtils.setField(controller, "blogService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BlogWriteExceptionHandler()).build();
        mvc.perform(post("/admin/blogs/" + action)
                        .param("blogId", "100").param("blogTitle", "标题")
                        .param("blogSubUrl", "article").param("blogCategoryId", "3")
                        .param("blogTags", "Java").param("blogContent", "正文")
                        .param("blogCoverImage", "/upload/cover.png")
                        .param("blogStatus", "1").param("enableComment", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(500))
                .andExpect(jsonPath("$.message").value("文章标签关系保存不完整"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"save", "update"})
    void databaseFailureAlsoKeepsAdminJsonContract(String action) throws Exception {
        BlogService service = mock(BlogService.class);
        when(service.saveBlog(any())).thenThrow(new DataIntegrityViolationException("模拟数据库异常"));
        when(service.updateBlog(any())).thenThrow(new DataIntegrityViolationException("模拟数据库异常"));
        BlogController controller = new BlogController();
        ReflectionTestUtils.setField(controller, "blogService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BlogWriteExceptionHandler()).build();
        mvc.perform(post("/admin/blogs/" + action)
                        .param("blogId", "100").param("blogTitle", "标题")
                        .param("blogSubUrl", "article").param("blogCategoryId", "3")
                        .param("blogTags", "Java").param("blogContent", "正文")
                        .param("blogCoverImage", "/upload/cover.png")
                        .param("blogStatus", "1").param("enableComment", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(500))
                .andExpect(jsonPath("$.message").value("数据保存失败，请稍后重试"));
    }

    @Test
    void databaseFailureOnPageRequestIsNotTurnedIntoJson() {
        BlogService service = mock(BlogService.class);
        when(service.getBlogById(any())).thenThrow(new DataIntegrityViolationException("模拟数据库异常"));
        BlogController controller = new BlogController();
        ReflectionTestUtils.setField(controller, "blogService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BlogWriteExceptionHandler()).build();
        Exception thrown = assertThrows(Exception.class, () -> mvc.perform(get("/admin/blogs/edit/100")));
        assertInstanceOf(DataIntegrityViolationException.class, thrown.getCause());
    }
}
