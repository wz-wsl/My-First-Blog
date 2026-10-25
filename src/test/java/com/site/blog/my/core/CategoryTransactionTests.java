package com.site.blog.my.core;

import com.site.blog.my.core.Exception.BlogWriteException;
import com.site.blog.my.core.GlobalExceptionHandler.BlogWriteExceptionHandler;
import com.site.blog.my.core.controller.admin.CategoryController;
import com.site.blog.my.core.dao.BlogCategoryMapper;
import com.site.blog.my.core.dao.BlogMapper;
import com.site.blog.my.core.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用真实事务代理和 MyBatis SQL，直接验证提交后的数据库状态。 */
@SpringJUnitConfig(BlogTransactionTests.Config.class)
class CategoryTransactionTests {
    @Autowired CategoryService service;
    @Autowired BlogMapper blogs;
    @Autowired BlogCategoryMapper categories;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepareDatabase() {
        reset(blogs, categories);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE tb_blog_category (
                  category_id INT PRIMARY KEY, category_name VARCHAR(50), category_icon VARCHAR(100),
                  category_rank INT DEFAULT 0, is_deleted TINYINT DEFAULT 0,
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE tb_blog (
                  blog_id BIGINT PRIMARY KEY, blog_category_id INT,
                  blog_category_name VARCHAR(50), is_deleted TINYINT DEFAULT 0)
                """);
        jdbc.update("INSERT INTO tb_blog_category(category_id, category_name, category_icon) VALUES (3, 'Java', 'old-icon'), (4, 'SQL', 'old-icon')");
        jdbc.update("INSERT INTO tb_blog(blog_id, blog_category_id, blog_category_name) VALUES (100, 3, 'Java'), (101, 4, 'SQL')");
    }

    @Test
    void updateZeroRowsRestoresArticleCategory() {
        doReturn(0).when(categories).updateByPrimaryKeySelective(any());
        assertThrows(BlogWriteException.class, () -> service.updateCategory(3, "新分类", "new-icon"));
        assertOriginalState();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void realSqlFailureRestoresArticleCategory(boolean delete) {
        if (delete) {
            jdbc.execute("ALTER TABLE tb_blog_category ADD CONSTRAINT deny_delete CHECK (is_deleted=0)");
            assertThrows(DataIntegrityViolationException.class, () -> service.deleteBatch(new Integer[]{3, 4}));
        } else {
            jdbc.execute("ALTER TABLE tb_blog_category ADD CONSTRAINT deny_rename CHECK (category_name <> '新分类')");
            assertThrows(DataIntegrityViolationException.class, () -> service.updateCategory(3, "新分类", "new-icon"));
        }
        assertOriginalState();
    }

    @Test
    void deleteZeroRowsRestoresArticleCategories() {
        doReturn(0).when(categories).deleteBatch(any());
        assertThrows(BlogWriteException.class, () -> service.deleteBatch(new Integer[]{3, 4}));
        assertOriginalState();
    }

    @Test
    void partialDeletionRollsBackDeletedCategoryAndArticleMoves() {
        doAnswer(invocation -> {
            // 模拟批量操作只成功删除一项，验证已经执行的两张表写入都能回滚。
            jdbc.update("UPDATE tb_blog_category SET is_deleted=1 WHERE category_id=3");
            return 1;
        }).when(categories).deleteBatch(any());
        assertThrows(BlogWriteException.class, () -> service.deleteBatch(new Integer[]{3, 4}));
        assertOriginalState();
    }

    @Test
    void successfulUpdateCommitsCategoryAndArticleName() {
        assertTrue(service.updateCategory(3, "新分类", "new-icon"));
        assertEquals("新分类", categoryName(3));
        assertEquals("new-icon", jdbc.queryForObject("SELECT category_icon FROM tb_blog_category WHERE category_id=3", String.class));
        assertEquals("新分类", articleCategoryName(100));
        assertEquals("SQL", articleCategoryName(101));
    }

    @Test
    void successfulDeleteMovesArticlesAndDeletesAllSelectedCategories() {
        assertTrue(service.deleteBatch(new Integer[]{3, 4}));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM tb_blog_category WHERE is_deleted=1", Integer.class));
        assertEquals(0, articleCategoryId(100));
        assertEquals(0, articleCategoryId(101));
        assertEquals("默认分类", articleCategoryName(100));
        assertEquals("默认分类", articleCategoryName(101));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void categoryWithoutArticlesCanStillBeUpdatedOrDeleted(boolean delete) {
        jdbc.update("DELETE FROM tb_blog");
        if (delete) assertTrue(service.deleteBatch(new Integer[]{3, 4}));
        else assertTrue(service.updateCategory(3, "新分类", "new-icon"));
    }

    @Test
    void duplicateIdsDoNotCauseFalsePartialDeletion() {
        assertTrue(service.deleteBatch(new Integer[]{3, 3, 4}));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM tb_blog_category WHERE is_deleted=1", Integer.class));
    }

    @Test
    void missingCategoryFailsBeforeMovingArticles() {
        assertThrows(BlogWriteException.class, () -> service.updateCategory(99, "新分类", "new-icon"));
        assertThrows(BlogWriteException.class, () -> service.deleteBatch(new Integer[]{3, 99}));
        assertOriginalState();
        verify(blogs, never()).updateBlogCategorys(anyString(), anyInt(), any());
    }

    @Test
    void deletedCategoryFailsBeforeMovingArticles() {
        jdbc.update("UPDATE tb_blog_category SET is_deleted=1 WHERE category_id=4");
        assertThrows(BlogWriteException.class, () -> service.deleteBatch(new Integer[]{3, 4}));
        assertEquals(3, articleCategoryId(100));
        assertEquals(4, articleCategoryId(101));
        verify(blogs, never()).updateBlogCategorys(anyString(), anyInt(), any());
    }

    @Test
    void invalidIdsFailBeforeWrites() {
        assertThrows(BlogWriteException.class, () -> service.deleteBatch(null));
        assertThrows(BlogWriteException.class, () -> service.deleteBatch(new Integer[0]));
        assertThrows(BlogWriteException.class, () -> service.deleteBatch(new Integer[]{3, null}));
        assertOriginalState();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failureResponseUsesGlobalHandlerAfterRollback(boolean delete) throws Exception {
        CategoryController controller = new CategoryController();
        ReflectionTestUtils.setField(controller, "categoryService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BlogWriteExceptionHandler()).build();
        if (delete) {
            doReturn(0).when(categories).deleteBatch(any());
            mvc.perform(post("/admin/categories/delete").contentType(MediaType.APPLICATION_JSON).content("[3,4]"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value(500))
                    .andExpect(jsonPath("$.message").value("分类删除不完整"));
        } else {
            doReturn(0).when(categories).updateByPrimaryKeySelective(any());
            mvc.perform(post("/admin/categories/update").param("categoryId", "3")
                            .param("categoryName", "新分类").param("categoryIcon", "new-icon"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value(500))
                    .andExpect(jsonPath("$.message").value("分类更新失败"));
        }
        assertOriginalState();
    }

    @Test
    void renamingToAnotherCategorysNameFailsBeforeWrites() {
        BlogWriteException exception = assertThrows(BlogWriteException.class,
                () -> service.updateCategory(4, "Java", "new-icon"));
        assertEquals("分类名称重复", exception.getMessage());
        assertEquals("SQL", categoryName(4));
        assertOriginalState();
        verify(blogs, never()).updateBlogCategorys(anyString(), anyInt(), any());
    }

    @Test
    void keepingOwnNameWhileChangingIconSucceeds() {
        assertTrue(service.updateCategory(3, "Java", "new-icon"));
        assertEquals("new-icon", jdbc.queryForObject("SELECT category_icon FROM tb_blog_category WHERE category_id=3", String.class));
    }

    @Test
    void controllerReportsDuplicateNameAndRejectsMissingIds() throws Exception {
        CategoryController controller = new CategoryController();
        ReflectionTestUtils.setField(controller, "categoryService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BlogWriteExceptionHandler()).build();
        mvc.perform(post("/admin/categories/update").param("categoryId", "4")
                        .param("categoryName", "Java").param("categoryIcon", "new-icon"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value(500))
                .andExpect(jsonPath("$.message").value("分类名称重复"));
        mvc.perform(post("/admin/categories/delete").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultCode").value(500))
                .andExpect(jsonPath("$.message").value("参数异常！"));
        assertOriginalState();
    }

    private void assertOriginalState() {
        assertEquals("Java", categoryName(3));
        assertEquals("SQL", categoryName(4));
        assertEquals("old-icon", jdbc.queryForObject("SELECT category_icon FROM tb_blog_category WHERE category_id=3", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tb_blog_category WHERE is_deleted=1", Integer.class));
        assertEquals(3, articleCategoryId(100));
        assertEquals(4, articleCategoryId(101));
        assertEquals("Java", articleCategoryName(100));
        assertEquals("SQL", articleCategoryName(101));
    }

    private String categoryName(int id) {
        return jdbc.queryForObject("SELECT category_name FROM tb_blog_category WHERE category_id=?", String.class, id);
    }
    private String articleCategoryName(int id) {
        return jdbc.queryForObject("SELECT blog_category_name FROM tb_blog WHERE blog_id=?", String.class, id);
    }
    private int articleCategoryId(int id) {
        return jdbc.queryForObject("SELECT blog_category_id FROM tb_blog WHERE blog_id=?", Integer.class, id);
    }
}
