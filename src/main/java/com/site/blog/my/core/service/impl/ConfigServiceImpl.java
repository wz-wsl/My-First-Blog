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
import java.util.function.Supplier;
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
        // 邮箱和备案号是可选项：空值表示不展示，不回填原作者的示例信息。
        Map<String, Supplier<String>> fallbacks = Map.of(
                "websiteName", defaults::getWebsiteName,
                "websiteDescription", defaults::getWebsiteDescription,
                "websiteLogo", defaults::getWebsiteLogo,
                "websiteIcon", defaults::getWebsiteIcon,
                "yourAvatar", defaults::getYourAvatar,
                "yourName", defaults::getYourName,
                "footerAbout", defaults::getFooterAbout,
                "footerCopyRight", defaults::getFooterCopyRight,
                "footerPoweredBy", defaults::getFooterPoweredBy,
                "footerPoweredByURL", defaults::getFooterPoweredByUrl);
        // 只回填数据库里已有但为空的配置项
        for (Map.Entry<String, String> config : configMap.entrySet()) {
            Supplier<String> fallback = fallbacks.get(config.getKey());
            if (fallback != null && !StringUtils.hasText(config.getValue())) {
                config.setValue(fallback.get());
            }
        }
        return configMap;
    }
}
