package com.site.blog.my.core;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import com.site.blog.my.core.controller.admin.AdminController;
import com.site.blog.my.core.controller.blog.MyBlogController;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.service.AdminUserService;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.service.CommentService;
import com.site.blog.my.core.util.CaptchaSessionUtil;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.HttpSessionMutexListener;
import org.springframework.web.util.WebUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CaptchaConcurrencyTests {
    /** 暂停第一个验证码读取，强制第二个请求撞上取出与删除之间的窗口。 */
    static class PausingSession extends MockHttpSession {
        final CountDownLatch firstRead = new CountDownLatch(1);
        final CountDownLatch secondRead = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger reads = new AtomicInteger();

        @Override
        public Object getAttribute(String name) {
            Object value = super.getAttribute(name);
            if ("verifyCode".equals(name)) {
                if (reads.incrementAndGet() == 1) {
                    firstRead.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("读取未释放");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                } else secondRead.countDown();
            }
            return value;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"login-login", "comment-comment", "login-comment"})
    void sameCaptchaCannotReachTwoOperations(String endpoints) throws Exception {
        PausingSession session = new PausingSession();
        new HttpSessionMutexListener().sessionCreated(new HttpSessionEvent(session));
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(150, 30, 4, 2);
        String code = captcha.getCode();
        CaptchaSessionUtil.store(session, captcha);
        AtomicInteger operations = new AtomicInteger();

        AdminUserService admins = mock(AdminUserService.class);
        when(admins.login(anyString(), anyString())).thenAnswer(call -> {
            operations.incrementAndGet();
            return null;
        });
        AdminController login = new AdminController();
        ReflectionTestUtils.setField(login, "adminUserService", admins);
        Blog blog = new Blog();
        blog.setBlogStatus((byte) 1);
        blog.setEnableComment((byte) 0);
        BlogService blogs = mock(BlogService.class);
        when(blogs.getBlogById(7L)).thenReturn(blog);
        CommentService comments = mock(CommentService.class);
        when(comments.addComment(any())).thenAnswer(call -> {
            operations.incrementAndGet();
            return true;
        });
        MyBlogController comment = new MyBlogController();
        ReflectionTestUtils.setField(comment, "blogService", blogs);
        ReflectionTestUtils.setField(comment, "commentService", comments);
        String[] paths = endpoints.split("-");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> submit(paths[0], session, code, login, comment));
            assertTrue(session.firstRead.await(5, TimeUnit.SECONDS));
            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                submit(paths[1], session, code, login, comment);
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            // 有锁时第二个请求不能读取；旧实现会在这里同时取得同一个验证码。
            session.secondRead.await(200, TimeUnit.MILLISECONDS);
            session.release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertEquals(1, operations.get());
            assertNull(CaptchaSessionUtil.consume(session));
        } finally {
            session.release.countDown();
            executor.shutdownNow();
        }
    }

    private void submit(String endpoint, MockHttpSession session, String code,
                        AdminController login, MyBlogController comment) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.addHeader("Referer", "http://localhost/blog/7");
        if (endpoint.equals("login")) login.login("admin", "secret", code, request, session);
        else comment.comment(request, session, 7L, code, "Visitor", "v@example.com", "", "Hello");
    }

    @Test
    void refreshDuringConsumptionDoesNotLoseTheNewChallenge() throws Exception {
        PausingSession session = new PausingSession();
        ShearCaptcha old = CaptchaUtil.createShearCaptcha(150, 30, 4, 2);
        ShearCaptcha next = CaptchaUtil.createShearCaptcha(150, 30, 4, 2);
        CaptchaSessionUtil.store(session, old);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ShearCaptcha> consuming = executor.submit(() -> CaptchaSessionUtil.consume(session));
            assertTrue(session.firstRead.await(5, TimeUnit.SECONDS));
            CountDownLatch refreshStarted = new CountDownLatch(1);
            CountDownLatch refreshFinished = new CountDownLatch(1);
            Future<?> refreshing = executor.submit(() -> {
                refreshStarted.countDown();
                CaptchaSessionUtil.store(session, next);
                refreshFinished.countDown();
            });
            assertTrue(refreshStarted.await(5, TimeUnit.SECONDS));
            assertFalse(refreshFinished.await(200, TimeUnit.MILLISECONDS));
            session.release.countDown();
            assertSame(old, consuming.get(5, TimeUnit.SECONDS));
            refreshing.get(5, TimeUnit.SECONDS);
            assertSame(next, CaptchaSessionUtil.consume(session));
            assertNull(CaptchaSessionUtil.consume(session));
        } finally {
            session.release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void sessionIdRotationKeepsMutexAndUnexpectedStateFailsClosed() {
        MockHttpSession session = new MockHttpSession();
        new HttpSessionMutexListener().sessionCreated(new HttpSessionEvent(session));
        Object mutex = WebUtils.getSessionMutex(session);
        session.changeSessionId();
        assertSame(mutex, WebUtils.getSessionMutex(session));
        session.setAttribute("verifyCode", "unexpected");
        assertNull(CaptchaSessionUtil.consume(session));
        assertNull(session.getAttribute("verifyCode"));
    }
}
