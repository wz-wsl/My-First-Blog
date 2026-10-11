package com.site.blog.my.core;

import com.site.blog.my.core.controller.admin.BlogController;
import com.site.blog.my.core.GlobalExceptionHandler.BlogWriteExceptionHandler;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.Exception.BlogWriteException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
}
