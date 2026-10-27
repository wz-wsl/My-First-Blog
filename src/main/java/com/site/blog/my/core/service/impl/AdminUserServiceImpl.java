package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.AdminUserMapper;
import com.site.blog.my.core.entity.AdminUser;
import com.site.blog.my.core.service.AdminUserService;
import com.site.blog.my.core.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    /** 旧版本保存的是 32 位小写十六进制 MD5 */
    private static final Pattern LEGACY_MD5 = Pattern.compile("^[0-9a-f]{32}$");

    @Resource
    private AdminUserMapper adminUserMapper;

    @Override
    public AdminUser login(String userName, String password) {
        AdminUser adminUser = adminUserMapper.selectActiveByLoginUserName(userName);
        if (adminUser == null || !matches(password, adminUser.getLoginPassword())) {
            return null;
        }
        if (isLegacyMd5(adminUser.getLoginPassword())) {
            upgradeLegacyPassword(adminUser, password);
        }
        return adminUser;
    }

    @Override
    public AdminUser getUserDetailById(Integer loginUserId) {
        return adminUserMapper.selectByPrimaryKey(loginUserId);
    }

    @Override
    public Boolean updatePassword(Integer loginUserId, String originalPassword, String newPassword) {
        AdminUser adminUser = adminUserMapper.selectByPrimaryKey(loginUserId);
        //当前用户非空且原密码正确才可以更改
        if (adminUser == null || !matches(originalPassword, adminUser.getLoginPassword())) {
            return false;
        }
        AdminUser update = new AdminUser();
        update.setAdminUserId(adminUser.getAdminUserId());
        update.setLoginPassword(PASSWORD_ENCODER.encode(newPassword));
        return adminUserMapper.updateByPrimaryKeySelective(update) > 0;
    }

    @Override
    public Boolean updateName(Integer loginUserId, String loginUserName, String nickName) {
        AdminUser adminUser = adminUserMapper.selectByPrimaryKey(loginUserId);
        //当前用户非空才可以进行更改
        if (adminUser != null) {
            //修改信息
            adminUser.setLoginUserName(loginUserName);
            adminUser.setNickName(nickName);
            if (adminUserMapper.updateByPrimaryKeySelective(adminUser) > 0) {
                //修改成功则返回true
                return true;
            }
        }
        return false;
    }

    /**
     * 同时兼容 BCrypt 和旧版 MD5，旧密码在下次登录时自动升级。
     */
    static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isLegacyMd5(storedPassword)) {
            String md5 = MD5Util.MD5Encode(rawPassword, "UTF-8");
            return md5 != null && MessageDigest.isEqual(
                    md5.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8));
        }
        try {
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isLegacyMd5(String storedPassword) {
        return LEGACY_MD5.matcher(storedPassword).matches();
    }

    private void upgradeLegacyPassword(AdminUser adminUser, String rawPassword) {
        AdminUser update = new AdminUser();
        update.setAdminUserId(adminUser.getAdminUserId());
        update.setLoginPassword(PASSWORD_ENCODER.encode(rawPassword));
        try {
            adminUserMapper.updateByPrimaryKeySelective(update);
        } catch (DataAccessException e) {
            // 升级失败不影响本次登录；常见原因是 login_password 列还是 varchar(50)。
            log.warn("管理员密码升级为 BCrypt 失败，请先执行 static-files/upgrade-password-column.sql", e);
        }
    }
}
