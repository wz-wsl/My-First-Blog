package com.site.blog.my.core;

import com.site.blog.my.core.controller.vo.BlogDetailVO;
import com.site.blog.my.core.dao.AdminUserMapper;
import com.site.blog.my.core.dao.BlogCategoryMapper;
import com.site.blog.my.core.dao.BlogCommentMapper;
import com.site.blog.my.core.dao.BlogMapper;
import com.site.blog.my.core.entity.AdminUser;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.service.impl.AdminUserServiceImpl;
import com.site.blog.my.core.service.impl.BlogServiceImpl;
import com.site.blog.my.core.util.PageQueryUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceFixesTests {

    // ---------- 密码 ----------

    private static final String MD5_OF_123456 = "e10adc3949ba59abbe56e057f20f883e";

    private static AdminUser admin(String storedPassword) {
        AdminUser user = new AdminUser();
        user.setAdminUserId(1);
        user.setLoginUserName("admin");
        user.setLoginPassword(storedPassword);
        user.setLocked((byte) 0);
        return user;
    }

    private static AdminUserServiceImpl adminService(AdminUserMapper mapper) {
        AdminUserServiceImpl service = new AdminUserServiceImpl();
        ReflectionTestUtils.setField(service, "adminUserMapper", mapper);
        return service;
    }

    @Test
    void legacyMd5PasswordStillLogsInAndIsUpgradedToBcrypt() {
        AdminUserMapper mapper = mock(AdminUserMapper.class);
        when(mapper.selectActiveByLoginUserName("admin")).thenReturn(admin(MD5_OF_123456));

        assertNotNull(adminService(mapper).login("admin", "123456"));

        ArgumentCaptor<AdminUser> update = ArgumentCaptor.forClass(AdminUser.class);
        verify(mapper).updateByPrimaryKeySelective(update.capture());
        String upgraded = update.getValue().getLoginPassword();
        assertTrue(upgraded.startsWith("$2"), "应保存为 BCrypt");
        assertTrue(new BCryptPasswordEncoder().matches("123456", upgraded));
        assertNull(update.getValue().getLoginUserName(), "只更新密码列");
    }

    @Test
    void upgradeFailureDoesNotBlockLogin() {
        AdminUserMapper mapper = mock(AdminUserMapper.class);
        when(mapper.selectActiveByLoginUserName("admin")).thenReturn(admin(MD5_OF_123456));
        when(mapper.updateByPrimaryKeySelective(any())).thenThrow(new DataIntegrityViolationException("列太短"));
        assertNotNull(adminService(mapper).login("admin", "123456"));
    }

    @Test
    void bcryptPasswordLogsInWithoutRewriting() {
        AdminUserMapper mapper = mock(AdminUserMapper.class);
        String hash = new BCryptPasswordEncoder().encode("s3cret");
        when(mapper.selectActiveByLoginUserName("admin")).thenReturn(admin(hash));

        assertNotNull(adminService(mapper).login("admin", "s3cret"));
        assertNull(adminService(mapper).login("admin", "wrong"));
        verify(mapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void unknownUserCannotLogIn() {
        AdminUserMapper mapper = mock(AdminUserMapper.class);
        assertNull(adminService(mapper).login("nobody", "123456"));
    }

    @Test
    void changingPasswordStoresBcryptAndChecksOldPassword() {
        AdminUserMapper mapper = mock(AdminUserMapper.class);
        when(mapper.selectByPrimaryKey(1)).thenReturn(admin(MD5_OF_123456));
        when(mapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        AdminUserServiceImpl service = adminService(mapper);

        assertFalse(service.updatePassword(1, "wrong", "new-pass"));
        assertTrue(service.updatePassword(1, "123456", "new-pass"));

        ArgumentCaptor<AdminUser> update = ArgumentCaptor.forClass(AdminUser.class);
        verify(mapper).updateByPrimaryKeySelective(update.capture());
        assertTrue(new BCryptPasswordEncoder().matches("new-pass", update.getValue().getLoginPassword()));
    }

    // ---------- 浏览量 ----------

    @Test
    void viewingArticleIncrementsViewsAtomicallyWithoutRewritingRow() {
        BlogMapper blogMapper = mock(BlogMapper.class);
        Blog blog = new Blog();
        blog.setBlogId(5L);
        blog.setBlogStatus((byte) 1);
        blog.setBlogViews(10L);
        blog.setBlogContent("# 标题");
        blog.setBlogCategoryId(0);
        when(blogMapper.selectByPrimaryKey(5L)).thenReturn(blog);
        BlogServiceImpl service = new BlogServiceImpl();
        ReflectionTestUtils.setField(service, "blogMapper", blogMapper);
        ReflectionTestUtils.setField(service, "categoryMapper", mock(BlogCategoryMapper.class));
        ReflectionTestUtils.setField(service, "blogCommentMapper", mock(BlogCommentMapper.class));

        BlogDetailVO detail = service.getBlogDetail(5L);

        verify(blogMapper).increaseBlogViews(5L);
        verify(blogMapper, never()).updateByPrimaryKey(any());
        verify(blogMapper, never()).updateByPrimaryKeySelective(any());
        assertEquals(11L, detail.getBlogViews());
    }

    // ---------- 分页 ----------

    private static PageQueryUtil page(Object page, Object limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("limit", limit);
        return new PageQueryUtil(params);
    }

    @Test
    void pageParametersAreClampedSoLimitOffsetIsNeverNegative() {
        assertEquals(0, page(0, 8).get("start"));
        assertEquals(0, page(-5, 8).get("start"));
        assertEquals(0, page("abc", "xyz").get("start"));
        assertEquals(10, page("abc", "xyz").getLimit());
        assertEquals(100, page(1, 100000).getLimit());
        assertEquals(1, page(1, 0).getLimit());
        assertTrue((int) page(Integer.MAX_VALUE, 100).get("start") >= 0);
        assertEquals(16, page(3, 8).get("start"));
    }
}
