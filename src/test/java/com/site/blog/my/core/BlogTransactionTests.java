package com.site.blog.my.core;

import com.site.blog.my.core.dao.*;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.service.CategoryService;
import com.site.blog.my.core.Exception.BlogWriteException;
import com.site.blog.my.core.service.impl.BlogServiceImpl;
import com.site.blog.my.core.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 使用真实 MyBatis SQL 和独立 H2 数据库；测试本身不加事务，直接观察 Service 提交/回滚。 */
@SpringJUnitConfig(BlogTransactionTests.Config.class)
class BlogTransactionTests {
    @Autowired BlogService service;
    @Autowired BlogMapper blogs;
    @Autowired BlogTagMapper tags;
    @Autowired BlogCategoryMapper categories;
    @Autowired BlogTagRelationMapper relations;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepareDatabase() {
        reset(blogs, tags, categories, relations);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE tb_blog (
                  blog_id BIGINT AUTO_INCREMENT PRIMARY KEY, blog_title VARCHAR(150),
                  blog_sub_url VARCHAR(150), blog_cover_image VARCHAR(200),
                  blog_category_id INT, blog_category_name VARCHAR(50), blog_tags VARCHAR(150),
                  blog_status TINYINT DEFAULT 1, blog_views BIGINT DEFAULT 0,
                  enable_comment TINYINT DEFAULT 1, is_deleted TINYINT DEFAULT 0,
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, blog_content TEXT)
                """);
        jdbc.execute("""
                CREATE TABLE tb_blog_category (
                  category_id INT PRIMARY KEY, category_name VARCHAR(50), category_icon VARCHAR(100),
                  category_rank INT, is_deleted TINYINT DEFAULT 0,
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE tb_blog_tag (
                  tag_id INT AUTO_INCREMENT PRIMARY KEY, tag_name VARCHAR(50),
                  is_deleted TINYINT DEFAULT 0, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE tb_blog_tag_relation (
                  relation_id BIGINT AUTO_INCREMENT PRIMARY KEY, blog_id BIGINT, tag_id INT,
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO tb_blog_category(category_id, category_name, category_rank) VALUES (3, 'Java', 5)");
        jdbc.update("INSERT INTO tb_blog_tag(tag_name) VALUES ('旧标签')");
    }

    @ParameterizedTest
    @ValueSource(strings = {"article", "tags", "category", "relationsZero", "relationsPartial", "sqlException", "missingTagId", "missingBlogId"})
    void saveFailureRollsBackEveryTable(String stage) {
        failAt(stage, false);
        assertWriteFailure(stage, () -> service.saveBlog(article(null)));
        assertEquals(0, count("tb_blog"));
        assertEquals(1, count("tb_blog_tag"));
        assertEquals(0, count("tb_blog_tag_relation"));
        assertEquals(5, rank());
    }

    @ParameterizedTest
    @ValueSource(strings = {"article", "tags", "category", "relationsZero", "relationsPartial", "sqlException", "missingTagId"})
    void updateFailureRestoresOldArticleAndRelations(String stage) {
        seedArticle();
        failAt(stage, true);
        assertWriteFailure(stage, () -> service.updateBlog(article(100L)));
        assertEquals("旧标题", jdbc.queryForObject("SELECT blog_title FROM tb_blog WHERE blog_id=100", String.class));
        assertEquals("旧标签", jdbc.queryForObject("SELECT blog_tags FROM tb_blog WHERE blog_id=100", String.class));
        assertEquals(1, count("tb_blog"));
        assertEquals(1, count("tb_blog_tag"));
        assertEquals(1, count("tb_blog_tag_relation"));
        assertEquals(1, jdbc.queryForObject("SELECT tag_id FROM tb_blog_tag_relation WHERE blog_id=100", Integer.class));
        assertEquals(5, rank());
    }

    @Test
    void successfulSaveCommitsGeneratedIdsAndAllRelations() {
        Blog article = article(null);
        assertEquals("success", service.saveBlog(article));
        assertNotNull(article.getBlogId());
        assertEquals(1, count("tb_blog"));
        assertEquals(3, count("tb_blog_tag"));
        assertEquals(2, count("tb_blog_tag_relation"));
        assertEquals(2, linkedNewTags());
        assertEquals(6, rank());
    }

    @Test
    void successfulUpdateCommitsReplacementRelations() {
        seedArticle();
        assertEquals("success", service.updateBlog(article(100L)));
        assertEquals("新标题", jdbc.queryForObject("SELECT blog_title FROM tb_blog WHERE blog_id=100", String.class));
        assertEquals("新标签一,新标签二", jdbc.queryForObject("SELECT blog_tags FROM tb_blog WHERE blog_id=100", String.class));
        assertEquals(2, count("tb_blog_tag_relation"));
        assertEquals(2, linkedNewTags());
        assertEquals(6, rank());
    }

    @Test
    void updateMissingArticleFailsWithoutWrites() {
        BlogWriteException exception = assertThrows(BlogWriteException.class,
                () -> service.updateBlog(article(100L)));
        assertEquals("数据不存在", exception.getMessage());
        assertEquals(0, count("tb_blog"));
        assertEquals(1, count("tb_blog_tag"));
        assertEquals(0, count("tb_blog_tag_relation"));
        assertEquals(5, rank());
        verify(relations, never()).deleteByBlogId(any());
    }

    @Test
    void updateTooManyTagsFailsBeforeWrites() {
        seedArticle();
        Blog article = article(100L);
        article.setBlogTags("a,b,c,d,e,f,g");
        BlogWriteException exception = assertThrows(BlogWriteException.class,
                () -> service.updateBlog(article));
        assertEquals("标签数量限制为6", exception.getMessage());
        assertEquals("旧标题", jdbc.queryForObject("SELECT blog_title FROM tb_blog WHERE blog_id=100", String.class));
        assertEquals(1, count("tb_blog_tag"));
        assertEquals(1, count("tb_blog_tag_relation"));
        assertEquals(5, rank());
        verify(relations, never()).deleteByBlogId(any());
    }

    @Test
    void updateWithoutOldRelationsCanSucceed() {
        seedArticle();
        jdbc.update("DELETE FROM tb_blog_tag_relation");
        assertEquals("success", service.updateBlog(article(100L)));
        assertEquals(2, linkedNewTags());
        assertEquals("新标题", jdbc.queryForObject("SELECT blog_title FROM tb_blog WHERE blog_id=100", String.class));
    }

    @Test
    void deleteExceptionRollsBackNewTagsAndCategoryUpdate() {
        seedArticle();
        doThrow(new DataIntegrityViolationException("模拟删除异常"))
                .when(relations).deleteByBlogId(100L);
        assertThrows(DataIntegrityViolationException.class, () -> service.updateBlog(article(100L)));
        assertEquals(1, count("tb_blog_tag"));
        assertEquals(5, rank());
        assertEquals(1, count("tb_blog_tag_relation"));
        assertEquals(1, jdbc.queryForObject("SELECT tag_id FROM tb_blog_tag_relation WHERE blog_id=100", Integer.class));
        assertEquals("旧标题", jdbc.queryForObject("SELECT blog_title FROM tb_blog WHERE blog_id=100", String.class));
    }

    @Test
    void validationFailureDoesNotWriteAnything() {
        Blog article = article(null);
        article.setBlogTags("a,b,c,d,e,f,g");
        assertEquals("标签数量限制为6", service.saveBlog(article));
        assertEquals(0, count("tb_blog"));
        assertEquals(1, count("tb_blog_tag"));
        assertEquals(0, count("tb_blog_tag_relation"));
        assertEquals(5, rank());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actualSqlConstraintFailureRollsBack(boolean update) {
        if (update) seedArticle();
        // 原标签 ID=1 合法；新标签违反约束，让真实 SQL（而非替身）失败。
        jdbc.execute("ALTER TABLE tb_blog_tag_relation ADD CONSTRAINT old_tag_only CHECK (tag_id=1)");
        assertThrows(DataIntegrityViolationException.class, () -> {
            if (update) service.updateBlog(article(100L));
            else service.saveBlog(article(null));
        });
        assertEquals(update ? 1 : 0, count("tb_blog"));
        assertEquals(1, count("tb_blog_tag"));
        assertEquals(update ? 1 : 0, count("tb_blog_tag_relation"));
        assertEquals(5, rank());
        if (update) {
            assertEquals("旧标题", jdbc.queryForObject("SELECT blog_title FROM tb_blog WHERE blog_id=100", String.class));
            assertEquals(1, jdbc.queryForObject("SELECT tag_id FROM tb_blog_tag_relation WHERE blog_id=100", Integer.class));
        }
    }

    private void assertWriteFailure(String stage, org.junit.jupiter.api.function.Executable action) {
        if (stage.equals("sqlException")) assertThrows(DataIntegrityViolationException.class, action);
        else assertThrows(BlogWriteException.class, action);
    }

    private void failAt(String stage, boolean update) {
        switch (stage) {
            case "article" -> {
                if (update) doReturn(0).when(blogs).updateByPrimaryKeySelective(any());
                else doReturn(0).when(blogs).insertSelective(any());
            }
            case "tags" -> doReturn(1).when(tags).batchInsertBlogTag(any());
            case "category" -> doReturn(0).when(categories).updateByPrimaryKeySelective(any());
            case "relationsZero" -> doReturn(0).when(relations).batchInsert(any());
            case "relationsPartial" -> doReturn(1).when(relations).batchInsert(any());
            case "sqlException" -> doThrow(new DataIntegrityViolationException("模拟数据库写入异常"))
                    .when(relations).batchInsert(any());
            case "missingTagId" -> doReturn(2).when(tags).batchInsertBlogTag(any());
            case "missingBlogId" -> doReturn(1).when(blogs).insertSelective(any());
            default -> throw new IllegalArgumentException(stage);
        }
    }

    private Blog article(Long id) {
        Blog blog = new Blog();
        blog.setBlogId(id);
        blog.setBlogTitle("新标题");
        blog.setBlogCategoryId(3);
        blog.setBlogTags("新标签一,新标签二");
        blog.setBlogContent("正文");
        return blog;
    }

    private void seedArticle() {
        jdbc.update("INSERT INTO tb_blog(blog_id, blog_title, blog_tags, blog_category_id) VALUES (100, '旧标题', '旧标签', 3)");
        jdbc.update("INSERT INTO tb_blog_tag_relation(blog_id, tag_id) VALUES (100, 1)");
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private int rank() {
        return jdbc.queryForObject("SELECT category_rank FROM tb_blog_category WHERE category_id=3", Integer.class);
    }

    private int linkedNewTags() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM tb_blog_tag_relation r
                JOIN tb_blog_tag t ON t.tag_id=r.tag_id
                JOIN tb_blog b ON b.blog_id=r.blog_id
                WHERE t.tag_name IN ('新标签一', '新标签二')
                """, Integer.class);
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean(destroyMethod = "shutdown")
        org.springframework.jdbc.datasource.embedded.EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2).build();
        }
        @Bean PlatformTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean SqlSessionTemplate sqlSessionTemplate(DataSource ds) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(ds);
            factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
            return new SqlSessionTemplate(factory.getObject());
        }
        @Bean BlogMapper blogMapper(SqlSessionTemplate sql) { return spy(sql.getMapper(BlogMapper.class)); }
        @Bean BlogTagMapper tagMapper(SqlSessionTemplate sql) { return spy(sql.getMapper(BlogTagMapper.class)); }
        @Bean BlogCategoryMapper categoryMapper(SqlSessionTemplate sql) { return spy(sql.getMapper(BlogCategoryMapper.class)); }
        @Bean BlogTagRelationMapper relationMapper(SqlSessionTemplate sql) { return spy(sql.getMapper(BlogTagRelationMapper.class)); }
        @Bean BlogCommentMapper commentMapper() { return mock(BlogCommentMapper.class); }
        @Bean BlogService blogService() { return new BlogServiceImpl(); }
        @Bean CategoryService categoryService() { return new CategoryServiceImpl(); }
    }
}
