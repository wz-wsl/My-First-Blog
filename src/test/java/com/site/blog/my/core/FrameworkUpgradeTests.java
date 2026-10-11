package com.site.blog.my.core;

import com.site.blog.my.core.controller.common.ErrorPageController;
import com.site.blog.my.core.controller.vo.BlogListVO;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.service.ConfigService;
import com.site.blog.my.core.service.TagService;
import com.site.blog.my.core.util.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证升级后的 MVC、模板、JSON 和上传契约；业务服务使用替身，避免修改真实博客数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FrameworkUpgradeTests {
    @TempDir
    static Path uploadDirectory;

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        // 同时覆盖上传控制器和资源映射，测试图片只写入临时目录。
        registry.add("config.FILE_UPLOAD_DIC", () -> uploadDirectory.toAbsolutePath() + "/");
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    ErrorPageController errorPageController;

    @MockitoBean
    BlogService blogService;
    @MockitoBean
    ConfigService configService;
    @MockitoBean
    TagService tagService;

    @BeforeEach
    void siteFixtures() {
        when(configService.getAllConfigs()).thenReturn(Map.ofEntries(
                Map.entry("websiteName", "升级测试博客"),
                Map.entry("websiteDescription", "框架升级验证"),
                Map.entry("websiteLogo", "/blog/amaze/images/avatar.png"),
                Map.entry("websiteIcon", "/blog/amaze/images/avatar.png"),
                Map.entry("yourAvatar", "/blog/amaze/images/avatar.png"),
                Map.entry("yourName", "测试作者"),
                Map.entry("yourEmail", ""),
                Map.entry("footerAbout", "测试站点"),
                Map.entry("footerCopyRight", "2026 测试"),
                Map.entry("footerICP", ""),
                Map.entry("footerPoweredBy", "My Blog"),
                Map.entry("footerPoweredByURL", "/")));
        when(tagService.getBlogTagCountForIndex()).thenReturn(List.of());
    }

    @Test
    void homeRendersThymeleafAndLombokProperties() throws Exception {
        BlogListVO article = new BlogListVO();
        article.setBlogId(42L);
        article.setBlogTitle("升级后的文章");
        article.setBlogCategoryName("开发实践");
        article.setBlogCoverImage("/blog/amaze/images/avatar.png");
        article.setCreateTime(new Date(0));
        when(blogService.getBlogsForIndexPage(1))
                .thenReturn(new PageResult(List.of(article), 1, 10, 1));
        when(blogService.getBlogListForIndexPage(0)).thenReturn(List.of());
        when(blogService.getBlogListForIndexPage(1)).thenReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("升级后的文章")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/blog/amaze/css/main.css")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("th:text="))));
    }

    @Test
    void adminRequiresLoginButLoginPageAndAssetsRemainAccessible() throws Exception {
        mvc.perform(get("/admin/blogs/list").servletPath("/admin/blogs/list")
                        .param("page", "1").param("limit", "10"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(get("/admin/login")).andExpect(status().isOk());
        mvc.perform(get("/blog/amaze/css/main.css")).andExpect(status().isOk());
    }

    @Test
    void jacksonKeepsAdminResultFieldsAndDateFormat() throws Exception {
        Blog article = new Blog();
        article.setBlogId(42L);
        article.setBlogTitle("测试文章");
        article.setCreateTime(new Date(0));
        when(blogService.getBlogsPage(any()))
                .thenReturn(new PageResult(List.of(article), 1, 10, 1));

        mvc.perform(get("/admin/blogs/list").servletPath("/admin/blogs/list")
                        .session(adminSession()).param("page", "1").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.list[0].blogId").value(42))
                .andExpect(jsonPath("$.data.list[0].createTime").value("1970-01-01 08:00:00"));
    }

    @Test
    void articleSaveStillBindsFormFields() throws Exception {
        when(blogService.saveBlog(any())).thenReturn("success");
        mvc.perform(post("/admin/blogs/save").servletPath("/admin/blogs/save")
                        .session(adminSession())
                        .param("blogTitle", "保存测试").param("blogSubUrl", "save-test")
                        .param("blogCategoryId", "1").param("blogTags", "Java")
                        .param("blogContent", "正文测试").param("blogCoverImage", "/cover.png")
                        .param("blogStatus", "0").param("enableComment", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value(200));
        var captured = org.mockito.ArgumentCaptor.forClass(Blog.class);
        verify(blogService).saveBlog(captured.capture());
        assertEquals("保存测试", captured.getValue().getBlogTitle());
        assertEquals(Byte.valueOf((byte) 0), captured.getValue().getBlogStatus());
    }

    @Test
    void coverUploadWritesFileAndServesSameBytes() throws Exception {
        byte[] bytes = testImage();
        String response = mvc.perform(multipart("/admin/upload/file")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", bytes))
                        .servletPath("/admin/upload/file").session(adminSession()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value(200))
                .andReturn().getResponse().getContentAsString();
        assertUploadedImage(jsonMapper.readTree(response).get("data").asString(), bytes);
    }

    @Test
    void markdownUploadPreservesEditorResponseAndImageMapping() throws Exception {
        byte[] bytes = testImage();
        String response = mvc.perform(multipart("/admin/blogs/md/uploadfile")
                        .file(new MockMultipartFile("editormd-image-file", "body.png", "image/png", bytes))
                        .servletPath("/admin/blogs/md/uploadfile").session(adminSession()))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn().getResponse().getContentAsString();
        var result = jsonMapper.readTree(response);
        assertEquals(1, result.get("success").intValue());
        assertUploadedImage(result.get("url").asString(), bytes);
    }

    @Test
    void captchaStillProducesPngAndStoresChallengeInSession() throws Exception {
        var result = mvc.perform(get("/common/kaptcha"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andReturn();
        assertNotNull(result.getRequest().getSession().getAttribute("verifyCode"));
        assertNotNull(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(
                result.getResponse().getContentAsByteArray())));
    }

    @Test
    void errorViewsUseBoot4ResolverContract() {
        var request = new MockHttpServletRequest();
        assertEquals("error/error_400", errorPageController
                .resolveErrorView(request, HttpStatus.BAD_REQUEST, Map.of()).getViewName());
        assertEquals("error/error_404", errorPageController
                .resolveErrorView(request, HttpStatus.NOT_FOUND, Map.of()).getViewName());
        assertEquals("error/error_5xx", errorPageController
                .resolveErrorView(request, HttpStatus.INTERNAL_SERVER_ERROR, Map.of()).getViewName());
    }

    private void assertUploadedImage(String url, byte[] expected) throws Exception {
        String path = URI.create(url).getPath();
        assertArrayEquals(expected, Files.readAllBytes(uploadDirectory.resolve(
                path.substring(path.lastIndexOf('/') + 1))));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().bytes(expected));
    }

    private byte[] testImage() throws Exception {
        try (var image = getClass().getResourceAsStream("/static/admin/dist/img/photo1.png")) {
            assertNotNull(image);
            return image.readAllBytes();
        }
    }

    private MockHttpSession adminSession() {
        // 构造测试会话，验证拦截器契约，无须登录真实管理员账号。
        var session = new MockHttpSession();
        session.setAttribute("loginUser", "test-admin");
        return session;
    }
}
