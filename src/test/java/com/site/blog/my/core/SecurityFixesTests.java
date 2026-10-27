package com.site.blog.my.core;

import cn.hutool.captcha.ShearCaptcha;
import com.site.blog.my.core.entity.AdminUser;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogComment;
import com.site.blog.my.core.service.AdminUserService;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.service.CommentService;
import com.site.blog.my.core.service.ConfigService;
import com.site.blog.my.core.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 审查修复的回归测试：验证码一次性、会话处理、评论校验、404 和上传白名单。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFixesTests {
    @TempDir
    static Path uploadDirectory;

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        registry.add("config.FILE_UPLOAD_DIC", () -> uploadDirectory.toAbsolutePath() + "/");
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BlogService blogService;
    @MockitoBean
    CommentService commentService;
    @MockitoBean
    AdminUserService adminUserService;
    @MockitoBean
    ConfigService configService;
    @MockitoBean
    TagService tagService;

    @BeforeEach
    void fixtures() {
        when(configService.getAllConfigs()).thenReturn(Map.of());
        when(tagService.getBlogTagCountForIndex()).thenReturn(List.of());
        when(commentService.addComment(any())).thenReturn(true);
    }

    // ---------- 验证码只能用一次 ----------

    @Test
    void loginCaptchaCannotBeReusedAfterFailedAttempt() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String code = loadCaptcha(session);
        when(adminUserService.login(anyString(), anyString())).thenReturn(null);

        mvc.perform(loginRequest(session, code)).andExpect(status().isOk());
        assertEquals("登陆失败", session.getAttribute("errorMsg"));

        // 同一个验证码再试一次，必须被拒绝，且不会再去比对密码
        mvc.perform(loginRequest(session, code)).andExpect(status().isOk());
        assertEquals("验证码错误", session.getAttribute("errorMsg"));
        verify(adminUserService, times(1)).login(anyString(), anyString());
    }

    @Test
    void commentCaptchaCannotBeReused() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String code = loadCaptcha(session);
        when(blogService.getBlogById(7L)).thenReturn(blog((byte) 1, (byte) 0));

        mvc.perform(commentRequest(session, code, "第一条评论"))
                .andExpect(jsonPath("$.resultCode").value(200));
        mvc.perform(commentRequest(session, code, "第二条评论"))
                .andExpect(jsonPath("$.message").value("验证码错误"));
        verify(commentService, times(1)).addComment(any());
    }

    // ---------- 会话 ----------

    @Test
    void successfulLoginChangesSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String code = loadCaptcha(session);
        AdminUser admin = new AdminUser();
        admin.setAdminUserId(1);
        admin.setNickName("管理员");
        when(adminUserService.login("admin", "secret")).thenReturn(admin);

        String before = session.getId();
        var request = mvc.perform(loginRequest(session, code))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest();
        assertNotEquals(before, request.getSession().getId());
        assertEquals(1, request.getSession().getAttribute("loginUserId"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", "管理员");
        mvc.perform(get("/admin/logout").servletPath("/admin/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
        assertTrue(session.isInvalid());
    }

    // ---------- 评论 ----------

    @Test
    void commentIsStoredVerbatimEvenAtLengthLimit() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(blogService.getBlogById(7L)).thenReturn(blog((byte) 1, (byte) 0));
        String body = "Let me create a Table from <b>Database</b> " + "<".repeat(157);
        assertEquals(200, body.length());

        mvc.perform(commentRequest(session, loadCaptcha(session), body))
                .andExpect(jsonPath("$.resultCode").value(200));
        ArgumentCaptor<BlogComment> saved = ArgumentCaptor.forClass(BlogComment.class);
        verify(commentService).addComment(saved.capture());
        // 不再篡改文字、不再膨胀长度；转义交给展示层
        assertEquals(body, saved.getValue().getCommentBody());
        assertEquals("Visitor", saved.getValue().getCommentator());
    }

    @ParameterizedTest
    @ValueSource(strings = {"draft", "commentsOff", "missing"})
    void commentIsRejectedWhenArticleDoesNotAcceptComments(String state) throws Exception {
        Blog blog = switch (state) {
            case "draft" -> blog((byte) 0, (byte) 0);
            case "commentsOff" -> blog((byte) 1, (byte) 1);
            default -> null;
        };
        when(blogService.getBlogById(7L)).thenReturn(blog);
        MockHttpSession session = new MockHttpSession();
        mvc.perform(commentRequest(session, loadCaptcha(session), "你好"))
                .andExpect(jsonPath("$.message").value("该文章不允许评论"));
        verify(commentService, never()).addComment(any());
    }

    @Test
    void commentatorLongerThanColumnIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(blogService.getBlogById(7L)).thenReturn(blog((byte) 1, (byte) 0));
        mvc.perform(commentRequest(session, loadCaptcha(session), "你好").param("commentator", "a".repeat(51)))
                .andExpect(jsonPath("$.message").value("称呼不能超过50个字"));
        verify(commentService, never()).addComment(any());
    }

    // ---------- 404 ----------

    @Test
    void missingArticleReturns404() throws Exception {
        when(blogService.getBlogDetail(999L)).thenReturn(null);
        mvc.perform(get("/blog/999")).andExpect(status().isNotFound());
    }

    @Test
    void unknownSubUrlReturns404() throws Exception {
        when(blogService.getBlogDetailBySubUrl("no-such-page")).thenReturn(null);
        mvc.perform(get("/no-such-page")).andExpect(status().isNotFound());
    }

    // ---------- 上传 ----------

    @ParameterizedTest
    @ValueSource(strings = {"evil.html", "icon.svg", "noextension", "shell.jsp"})
    void uploadRejectsNonImageFiles(String fileName) throws Exception {
        long before = countUploads();
        mvc.perform(multipart("/admin/upload/file")
                        .file(new MockMultipartFile("file", fileName, "text/html", "<script>x</script>".getBytes()))
                        .servletPath("/admin/upload/file").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(500))
                .andExpect(jsonPath("$.message").value("只支持上传 jpg、jpeg、png、gif、webp 格式的图片"));
        assertEquals(before, countUploads());
    }

    @Test
    void editorUploadRejectsNonImageFiles() throws Exception {
        mvc.perform(multipart("/admin/blogs/md/uploadfile")
                        .file(new MockMultipartFile("editormd-image-file", "evil.html", "text/html", "x".getBytes()))
                        .servletPath("/admin/blogs/md/uploadfile").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(0));
    }

    @Test
    void uploadsWithSameSecondDoNotOverwriteEachOther() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(multipart("/admin/upload/file")
                            .file(new MockMultipartFile("file", "a.PNG", "image/png", new byte[]{(byte) i}))
                            .servletPath("/admin/upload/file").session(adminSession()))
                    .andExpect(jsonPath("$.resultCode").value(200))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.endsWith(".png")));
        }
        try (var files = Files.list(uploadDirectory)) {
            assertTrue(files.filter(p -> p.toString().endsWith(".png")).count() >= 5);
        }
    }

    // ---------- 工具方法 ----------

    private String loadCaptcha(MockHttpSession session) throws Exception {
        mvc.perform(get("/common/kaptcha").session(session)).andExpect(status().isOk());
        return ((ShearCaptcha) session.getAttribute("verifyCode")).getCode();
    }

    private MockHttpServletRequestBuilder loginRequest(MockHttpSession session, String code) {
        return post("/admin/login").servletPath("/admin/login").session(session)
                .param("userName", "admin").param("password", "secret").param("verifyCode", code);
    }

    private MockHttpServletRequestBuilder commentRequest(MockHttpSession session, String code, String body) {
        return post("/blog/comment").session(session).header("Referer", "http://localhost/blog/7")
                .param("blogId", "7").param("verifyCode", code)
                .param("commentator", " Visitor ").param("email", "visitor@example.com")
                .param("websiteUrl", "").param("commentBody", body);
    }

    private static Blog blog(byte status, byte enableComment) {
        Blog blog = new Blog();
        blog.setBlogId(7L);
        blog.setBlogStatus(status);
        blog.setEnableComment(enableComment);
        blog.setIsDeleted((byte) 0);
        return blog;
    }

    private static long countUploads() throws Exception {
        try (var files = Files.list(uploadDirectory)) {
            return files.count();
        }
    }

    private static MockHttpSession adminSession() {
        var session = new MockHttpSession();
        session.setAttribute("loginUser", "test-admin");
        return session;
    }
}
