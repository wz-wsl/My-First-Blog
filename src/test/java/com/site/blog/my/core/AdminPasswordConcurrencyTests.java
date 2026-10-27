package com.site.blog.my.core;

import com.site.blog.my.core.dao.AdminUserMapper;
import com.site.blog.my.core.entity.AdminUser;
import com.site.blog.my.core.service.impl.AdminUserServiceImpl;
import com.site.blog.my.core.util.MD5Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用真实 Mapper SQL，按可控顺序模拟读取旧值后发生的并发写入。 */
class AdminPasswordConcurrencyTests {
    EmbeddedDatabase database;
    JdbcTemplate jdbc;
    AdminUserMapper mapper;
    AdminUserServiceImpl service;
    String oldHash;
    String newHash;

    @BeforeEach
    void database() throws Exception {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(database);
        jdbc.execute("CREATE TABLE tb_admin_user (admin_user_id INT PRIMARY KEY, login_user_name VARCHAR(50), "
                + "login_password VARCHAR(100), nick_name VARCHAR(50), locked TINYINT)");
        oldHash = MD5Util.MD5Encode("old-password", "UTF-8");
        newHash = new BCryptPasswordEncoder().encode("new-password");
        jdbc.update("INSERT INTO tb_admin_user VALUES (1, 'admin', ?, 'Admin', 0)", oldHash);
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(database);
        factory.setMapperLocations(new ClassPathResource("mapper/AdminUserMapper.xml"));
        mapper = spy(new SqlSessionTemplate(factory.getObject()).getMapper(AdminUserMapper.class));
        service = new AdminUserServiceImpl();
        ReflectionTestUtils.setField(service, "adminUserMapper", mapper);
    }

    @AfterEach
    void close() {
        if (database != null) database.shutdown();
    }

    private String stored() {
        return jdbc.queryForObject("SELECT login_password FROM tb_admin_user WHERE admin_user_id=1", String.class);
    }

    @Test
    void ordinaryLegacyLoginMigratesAndCanLogInAgain() {
        assertNotNull(service.login("admin", "old-password"));
        assertTrue(stored().startsWith("$2"));
        assertNotNull(service.login("admin", "old-password"));
        assertNull(service.login("admin", "wrong-password"));
    }

    @Test
    void longLegacyPasswordCanBeChangedToBcryptAtTheByteLimit() {
        String longPassword = "密".repeat(25);
        String hash = MD5Util.MD5Encode(longPassword, "UTF-8");
        jdbc.update("UPDATE tb_admin_user SET login_password=? WHERE admin_user_id=1", hash);
        assertNotNull(service.login("admin", longPassword));
        assertEquals(hash, stored());
        String atLimit = "密".repeat(24);
        assertTrue(service.updatePassword(1, longPassword, atLimit));
        assertTrue(stored().startsWith("$2"));
        assertNotNull(service.login("admin", atLimit));
        assertNull(service.login("admin", longPassword));
    }

    @Test
    void mappedPasswordUpdateRequiresTheExpectedHash() {
        assertEquals(1, mapper.updatePasswordIfUnchanged(1, oldHash, newHash));
        assertEquals(0, mapper.updatePasswordIfUnchanged(1, oldHash, "stale"));
        assertEquals(0, mapper.updatePasswordIfUnchanged(99, newHash, "wrong-user"));
        assertEquals(newHash, stored());
    }

    @Test
    void migrationDoesNotOverwritePasswordChangedAfterLoginRead() {
        AdminUser snapshot = mapper.selectActiveByLoginUserName("admin");
        doAnswer(call -> {
            jdbc.update("UPDATE tb_admin_user SET login_password=? WHERE admin_user_id=1", newHash);
            return snapshot;
        }).when(mapper).selectActiveByLoginUserName("admin");
        assertNotNull(service.login("admin", "old-password"));
        assertEquals(newHash, stored());
    }

    @Test
    void stalePasswordChangeIsRejected() {
        AdminUser snapshot = mapper.selectByPrimaryKey(1);
        doAnswer(call -> {
            jdbc.update("UPDATE tb_admin_user SET login_password=? WHERE admin_user_id=1", newHash);
            return snapshot;
        }).when(mapper).selectByPrimaryKey(1);
        assertFalse(service.updatePassword(1, "old-password", "another-password"));
        assertEquals(newHash, stored());
    }

    @Test
    void nameChangeDoesNotReplayPasswordOrLockedState() {
        AdminUser snapshot = mapper.selectByPrimaryKey(1);
        doAnswer(call -> {
            jdbc.update("UPDATE tb_admin_user SET login_password=?, locked=1 WHERE admin_user_id=1", newHash);
            return snapshot;
        }).when(mapper).selectByPrimaryKey(1);
        assertTrue(service.updateName(1, "renamed", "New nickname"));
        assertEquals(newHash, stored());
        assertEquals(1, jdbc.queryForObject("SELECT locked FROM tb_admin_user", Integer.class));
        assertEquals("renamed", jdbc.queryForObject("SELECT login_user_name FROM tb_admin_user", String.class));
    }
}
