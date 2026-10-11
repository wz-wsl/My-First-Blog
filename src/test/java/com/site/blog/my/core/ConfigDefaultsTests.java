package com.site.blog.my.core;

import com.site.blog.my.core.config.BlogDefaultsProperties;
import com.site.blog.my.core.dao.BlogConfigMapper;
import com.site.blog.my.core.entity.BlogConfig;
import com.site.blog.my.core.service.ConfigService;
import com.site.blog.my.core.service.impl.ConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigDefaultsTests {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(Config.class);

    @Test
    void applicationYamlBindsDefaultsIncludingChineseAndEmptyValues() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            BlogDefaultsProperties defaults = context.getBean(BlogDefaultsProperties.class);
            assertEquals("慢写手记", defaults.getWebsiteName());
            assertEquals("记录开发中的小问题，也记录生活里的小发现。把复杂的事情讲清楚，把值得留下的想法慢慢写下来。", defaults.getWebsiteDescription());
            assertEquals("/admin/dist/img/logo2.png", defaults.getWebsiteLogo());
            assertEquals("/admin/dist/img/favicon.png", defaults.getWebsiteIcon());
            assertEquals("/admin/dist/img/avatar.png", defaults.getYourAvatar());
            assertEquals("", defaults.getYourEmail());
            assertEquals("AshenOne", defaults.getYourName());
            assertEquals("写代码，也写生活。保持好奇，慢慢积累。", defaults.getFooterAbout());
            assertEquals("", defaults.getFooterIcp());
            assertEquals("© 2026 慢写手记", defaults.getFooterCopyRight());
            assertEquals("My Blog", defaults.getFooterPoweredBy());
            assertEquals("https://github.com/ZHENFENG13/My-Blog", defaults.getFooterPoweredByUrl());
        });
    }

    @Test
    void databaseValuesTakePriorityAndBlankValuesUseConfiguredDefaults() {
        runner.withPropertyValues("blog.defaults.website-name=覆盖后的默认站点").run(context -> {
            BlogConfig databaseName = row("websiteName", "   ");
            BlogConfig databaseAuthor = row("yourName", "数据库作者");
            when(context.getBean(BlogConfigMapper.class).selectAll()).thenReturn(List.of(
                    databaseName, databaseAuthor, row("footerCopyRight", ""), row("footerPoweredByURL", "")));
            Map<String, String> result = context.getBean(ConfigService.class).getAllConfigs();
            assertEquals("覆盖后的默认站点", result.get("websiteName"));
            assertEquals("数据库作者", result.get("yourName"));
            assertEquals("© 2026 慢写手记", result.get("footerCopyRight"));
            assertEquals("https://github.com/ZHENFENG13/My-Blog", result.get("footerPoweredByURL"));
            assertFalse(result.containsKey("websiteIcon"));
            // 实体 setter 原本会 trim；读取默认值后也不应把实体改成默认站点名。
            assertEquals("", databaseName.getConfigValue());
            verify(context.getBean(BlogConfigMapper.class), never()).updateByPrimaryKeySelective(any());
        });
    }

    @Test
    void optionalEmailAndIcpStayEmptyEvenWhenDefaultsAreConfigured() {
        runner.withPropertyValues("blog.defaults.your-email=fallback@example.com", "blog.defaults.footer-icp=示例备案")
                .run(context -> {
                    when(context.getBean(BlogConfigMapper.class).selectAll()).thenReturn(List.of(
                            row("yourEmail", ""), row("footerICP", "")));
                    Map<String, String> result = context.getBean(ConfigService.class).getAllConfigs();
                    assertEquals("", result.get("yourEmail"));
                    assertEquals("", result.get("footerICP"));
                });
    }

    private static BlogConfig row(String name, String value) {
        BlogConfig config = new BlogConfig();
        config.setConfigName(name);
        config.setConfigValue(value);
        return config;
    }

    @Configuration
    @EnableConfigurationProperties(BlogDefaultsProperties.class)
    @Import(ConfigServiceImpl.class)
    static class Config {
        @Bean BlogConfigMapper configMapper() { return mock(BlogConfigMapper.class); }
    }
}
