package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.config.BlogDefaultsProperties;
import com.site.blog.my.core.dao.BlogConfigMapper;
import com.site.blog.my.core.entity.BlogConfig;
import com.site.blog.my.core.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {
    @Autowired
    private BlogConfigMapper configMapper;

    @Autowired
    private BlogDefaultsProperties defaults;

    @Override
    public int updateConfig(String configName, String configValue) {
        BlogConfig blogConfig = configMapper.selectByPrimaryKey(configName);
        if (blogConfig != null) {
            blogConfig.setConfigValue(configValue);
            blogConfig.setUpdateTime(new Date());
            return configMapper.updateByPrimaryKeySelective(blogConfig);
        }
        return 0;
    }

    @Override
    public Map<String, String> getAllConfigs() {
        //获取所有的map并封装为map
        List<BlogConfig> blogConfigs = configMapper.selectAll();
        Map<String, String> configMap = blogConfigs.stream().collect(Collectors.toMap(BlogConfig::getConfigName, BlogConfig::getConfigValue));
        for (Map.Entry<String, String> config : configMap.entrySet()) {
            if ("websiteName".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getWebsiteName());
            }
            if ("websiteDescription".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getWebsiteDescription());
            }
            if ("websiteLogo".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getWebsiteLogo());
            }
            if ("websiteIcon".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getWebsiteIcon());
            }
            if ("yourAvatar".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getYourAvatar());
            }
            // 邮箱和备案号是可选项：空值表示不展示，不回填原作者的示例信息。
            if ("yourName".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getYourName());
            }
            if ("footerAbout".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getFooterAbout());
            }
            if ("footerCopyRight".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getFooterCopyRight());
            }
            if ("footerPoweredBy".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getFooterPoweredBy());
            }
            if ("footerPoweredByURL".equals(config.getKey()) && !StringUtils.hasText(config.getValue())) {
                config.setValue(defaults.getFooterPoweredByUrl());
            }
        }
        return configMap;
    }
}
