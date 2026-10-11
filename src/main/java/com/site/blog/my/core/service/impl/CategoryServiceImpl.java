package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.BlogCategoryMapper;
import com.site.blog.my.core.dao.BlogMapper;
import com.site.blog.my.core.entity.BlogCategory;
import com.site.blog.my.core.Exception.BlogWriteException;
import com.site.blog.my.core.service.CategoryService;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private BlogCategoryMapper blogCategoryMapper;
    @Autowired
    private BlogMapper blogMapper;

    @Override
    public PageResult getBlogCategoryPage(PageQueryUtil pageUtil) {
        List<BlogCategory> categoryList = blogCategoryMapper.findCategoryList(pageUtil);
        int total = blogCategoryMapper.getTotalCategories(pageUtil);
        PageResult pageResult = new PageResult(categoryList, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    public int getTotalCategories() {
        return blogCategoryMapper.getTotalCategories(null);
    }

    @Override
    public Boolean saveCategory(String categoryName, String categoryIcon) {
        BlogCategory temp = blogCategoryMapper.selectByCategoryName(categoryName);
        if (temp == null) {
            BlogCategory blogCategory = new BlogCategory();
            blogCategory.setCategoryName(categoryName);
            blogCategory.setCategoryIcon(categoryIcon);
            return blogCategoryMapper.insertSelective(blogCategory) > 0;
        }
        return false;
    }

    @Override
    @Transactional
    public Boolean updateCategory(Integer categoryId, String categoryName, String categoryIcon) {
        BlogCategory blogCategory = blogCategoryMapper.selectByPrimaryKey(categoryId);
        if (blogCategory == null) {
            throw new BlogWriteException("分类不存在");
        }
        blogCategory.setCategoryIcon(categoryIcon);
        blogCategory.setCategoryName(categoryName);
        // 分类没有文章时更新 0 行也正常；SQL 异常向外传播，触发整体回滚。
        blogMapper.updateBlogCategorys(categoryName, blogCategory.getCategoryId(), new Integer[]{categoryId});
        if (blogCategoryMapper.updateByPrimaryKeySelective(blogCategory) != 1) {
            throw new BlogWriteException("分类更新失败");
        }
        return true;
    }

    @Override
    @Transactional
    public Boolean deleteBatch(Integer[] ids) {
        if (ids == null || ids.length == 0 || Arrays.stream(ids).anyMatch(id -> id == null)) {
            throw new BlogWriteException("请选择有效的分类");
        }
        Integer[] categoryIds = Arrays.stream(ids).distinct().toArray(Integer[]::new);
        if (blogCategoryMapper.selectByCategoryIds(Arrays.asList(categoryIds)).size() != categoryIds.length) {
            throw new BlogWriteException("部分分类不存在或已删除");
        }
        //修改tb_blog表
        blogMapper.updateBlogCategorys("默认分类", 0, categoryIds);
        //删除分类数据
        if (blogCategoryMapper.deleteBatch(categoryIds) != categoryIds.length) {
            throw new BlogWriteException("分类删除不完整");
        }
        return true;
    }

    @Override
    public List<BlogCategory> getAllCategories() {
        return blogCategoryMapper.findCategoryList(null);
    }

}
