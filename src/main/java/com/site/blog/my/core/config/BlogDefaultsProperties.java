package com.site.blog.my.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 从 application.yml 的 blog.defaults 读取站点默认值。 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.defaults")
public class BlogDefaultsProperties {
    private String websiteName;
    private String websiteDescription;
    private String websiteLogo;
    private String websiteIcon;
    private String yourAvatar;
    private String yourEmail;
    private String yourName;
    private String footerAbout;
    private String footerIcp;
    private String footerCopyRight;
    private String footerPoweredBy;
    private String footerPoweredByUrl;
}
