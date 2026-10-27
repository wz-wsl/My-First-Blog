package com.site.blog.my.core.controller.common;

import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 按状态码选择错误页模板，并保留原本的 HTTP 状态码。
 */
@Component
public class ErrorPageController implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if (HttpStatus.BAD_REQUEST == status) {
            return new ModelAndView("error/error_400", model, status);
        } else if (HttpStatus.NOT_FOUND == status) {
            return new ModelAndView("error/error_404", model, status);
        } else {
            return new ModelAndView("error/error_5xx", model, status);
        }
    }
}
