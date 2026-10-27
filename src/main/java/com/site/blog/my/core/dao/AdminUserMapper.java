package com.site.blog.my.core.dao;

import com.site.blog.my.core.entity.AdminUser;
import org.apache.ibatis.annotations.Param;

public interface AdminUserMapper {
    int insert(AdminUser record);

    int insertSelective(AdminUser record);

    /**
     * 按用户名查询未锁定的管理员，密码在 Service 中比对
     */
    AdminUser selectActiveByLoginUserName(@Param("userName") String userName);

    AdminUser selectByPrimaryKey(Integer adminUserId);

    int updateByPrimaryKeySelective(AdminUser record);

    /** 仅在密码未被其他请求修改时更新，返回 0 表示原哈希已过期。 */
    int updatePasswordIfUnchanged(@Param("adminUserId") Integer adminUserId,
                                  @Param("expectedPassword") String expectedPassword,
                                  @Param("newPassword") String newPassword);

    int updateByPrimaryKey(AdminUser record);
}
