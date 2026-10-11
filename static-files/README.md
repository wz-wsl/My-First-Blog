# 当前项目资料

本目录已更新为“慢写手记”的实际页面和当前数据库快照，更新日期为 2026-10-11。

## 页面图片

| 文件 | 内容 |
| --- | --- |
| `personal-blog.png` | 前台首页介绍区域 |
| `index03.png` | 前台首页与最新文章 |
| `detail03.png` | “一篇好用的 Markdown 笔记怎么写”详情页 |
| `link03.png` | 前台友情链接 |
| `login.png` | 后台登录页 |
| `dashboard.png`、`my-blog-info.png` | 后台概览和当前内容统计 |
| `blog-list.png` | 文章管理 |
| `edit.png`、`edit-content.png` | 当前文章的编辑表单和 Markdown 编辑器 |
| `comment-list.png` | 评论管理 |
| `link-list.png` | 友链管理 |
| `config.png` | 当前站点配置 |
| `cover-img.png` | 当前最新发布文章使用的封面图 |

截图来自本地运行页面，沿用原有文件名，主 README 中的图片引用仍可使用。

## 数据库快照

`my_blog_db.sql` 包含当前数据库的真实建表语句和全部记录，包括草稿、软删除记录和管理员数据。

| 表 | 记录数 |
| --- | ---: |
| `tb_admin_user` | 1 |
| `tb_blog` | 13 |
| `tb_blog_category` | 5 |
| `tb_blog_comment` | 2 |
| `tb_blog_tag` | 17 |
| `tb_blog_tag_relation` | 26 |
| `tb_config` | 12 |
| `tb_link` | 6 |
| 合计 | 82 |

已在独立临时数据库中完成导入验证，8 张表的记录数和逐字段数据均与源库一致，验证库已清理。

导入脚本会创建 `my_blog_db`（若不存在），并重建其中的 8 张表；请先备份目标库。登录账号及密码哈希沿用导出时的管理员数据。

在 MySQL 客户端中执行：

```sql
source D:/Program/My-Blog-master/static-files/my_blog_db.sql;
```

SQL 保存图片引用路径，不包含上传文件本身；迁移站点时还需备份实际上传目录，并配置 `config.FILE_UPLOAD_DIC`。
