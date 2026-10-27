package com.site.blog.my.core.util;

import cn.hutool.captcha.ShearCaptcha;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.util.WebUtils;

/** 验证码的替换与一次性取出共用会话锁；锁内不进行密码校验或数据库操作。 */
public final class CaptchaSessionUtil {
    private static final String ATTRIBUTE = "verifyCode";

    private CaptchaSessionUtil() {
    }

    public static void store(HttpSession session, ShearCaptcha captcha) {
        synchronized (WebUtils.getSessionMutex(session)) {
            session.setAttribute(ATTRIBUTE, captcha);
        }
    }

    public static ShearCaptcha consume(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object captcha = session.getAttribute(ATTRIBUTE);
            session.removeAttribute(ATTRIBUTE);
            return captcha instanceof ShearCaptcha challenge ? challenge : null;
        }
    }
}
