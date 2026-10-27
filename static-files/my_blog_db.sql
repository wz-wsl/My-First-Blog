-- Current database snapshot: my_blog_db
-- Exported: 2026-10-11T21:44:14.955803200+08:00[Asia/Shanghai]
-- Contains current table definitions and all rows, including administrator password hashes.

SET NAMES utf8mb4;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_TIME_ZONE=@@TIME_ZONE, TIME_ZONE='+00:00';
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

CREATE DATABASE IF NOT EXISTS `my_blog_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `my_blog_db`;

-- Table: tb_admin_user
DROP TABLE IF EXISTS `tb_admin_user`;
CREATE TABLE `tb_admin_user` (
  `admin_user_id` int NOT NULL AUTO_INCREMENT COMMENT '管理员id',
  `login_user_name` varchar(50) NOT NULL COMMENT '管理员登陆名称',
  `login_password` varchar(100) NOT NULL COMMENT '管理员登陆密码（BCrypt）',
  `nick_name` varchar(50) NOT NULL COMMENT '管理员显示昵称',
  `locked` tinyint DEFAULT '0' COMMENT '是否锁定 0未锁定 1已锁定无法登陆',
  PRIMARY KEY (`admin_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3;

-- Table: tb_blog
DROP TABLE IF EXISTS `tb_blog`;
CREATE TABLE `tb_blog` (
  `blog_id` bigint NOT NULL AUTO_INCREMENT COMMENT '博客表主键id',
  `blog_title` varchar(200) NOT NULL COMMENT '博客标题',
  `blog_sub_url` varchar(200) NOT NULL COMMENT '博客自定义路径url',
  `blog_cover_image` varchar(200) NOT NULL COMMENT '博客封面图',
  `blog_content` mediumtext NOT NULL COMMENT '博客内容',
  `blog_category_id` int NOT NULL COMMENT '博客分类id',
  `blog_category_name` varchar(50) NOT NULL COMMENT '博客分类(冗余字段)',
  `blog_tags` varchar(200) NOT NULL COMMENT '博客标签',
  `blog_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-草稿 1-发布',
  `blog_views` bigint NOT NULL DEFAULT '0' COMMENT '阅读量',
  `enable_comment` tinyint NOT NULL DEFAULT '0' COMMENT '0-允许评论 1-不允许评论',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除 0=否 1=是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`blog_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb3;

-- Table: tb_blog_category
DROP TABLE IF EXISTS `tb_blog_category`;
CREATE TABLE `tb_blog_category` (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '分类表主键',
  `category_name` varchar(50) NOT NULL COMMENT '分类的名称',
  `category_icon` varchar(50) NOT NULL COMMENT '分类的图标',
  `category_rank` int NOT NULL DEFAULT '1' COMMENT '分类的排序值 被使用的越多数值越大',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除 0=否 1=是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb3;

-- Table: tb_blog_comment
DROP TABLE IF EXISTS `tb_blog_comment`;
CREATE TABLE `tb_blog_comment` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `blog_id` bigint NOT NULL DEFAULT '0' COMMENT '关联的blog主键',
  `commentator` varchar(50) NOT NULL DEFAULT '' COMMENT '评论者名称',
  `email` varchar(100) NOT NULL DEFAULT '' COMMENT '评论人的邮箱',
  `website_url` varchar(50) NOT NULL DEFAULT '' COMMENT '网址',
  `comment_body` varchar(200) NOT NULL DEFAULT '' COMMENT '评论内容',
  `comment_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论提交时间',
  `commentator_ip` varchar(20) NOT NULL DEFAULT '' COMMENT '评论时的ip地址',
  `reply_body` varchar(200) NOT NULL DEFAULT '' COMMENT '回复内容',
  `reply_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
  `comment_status` tinyint NOT NULL DEFAULT '0' COMMENT '是否审核通过 0-未审核 1-审核通过',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除 0-未删除 1-已删除',
  PRIMARY KEY (`comment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb3;

-- Table: tb_blog_tag
DROP TABLE IF EXISTS `tb_blog_tag`;
CREATE TABLE `tb_blog_tag` (
  `tag_id` int NOT NULL AUTO_INCREMENT COMMENT '标签表主键id',
  `tag_name` varchar(100) NOT NULL COMMENT '标签名称',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除 0=否 1=是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`tag_id`)
) ENGINE=InnoDB AUTO_INCREMENT=153 DEFAULT CHARSET=utf8mb3;

-- Table: tb_blog_tag_relation
DROP TABLE IF EXISTS `tb_blog_tag_relation`;
CREATE TABLE `tb_blog_tag_relation` (
  `relation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系表id',
  `blog_id` bigint NOT NULL COMMENT '博客id',
  `tag_id` int NOT NULL COMMENT '标签id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`relation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=309 DEFAULT CHARSET=utf8mb3;

-- Table: tb_config
DROP TABLE IF EXISTS `tb_config`;
CREATE TABLE `tb_config` (
  `config_name` varchar(100) NOT NULL DEFAULT '' COMMENT '配置项的名称',
  `config_value` varchar(200) NOT NULL DEFAULT '' COMMENT '配置项的值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`config_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- Table: tb_link
DROP TABLE IF EXISTS `tb_link`;
CREATE TABLE `tb_link` (
  `link_id` int NOT NULL AUTO_INCREMENT COMMENT '友链表主键id',
  `link_type` tinyint NOT NULL DEFAULT '0' COMMENT '友链类别 0-友链 1-推荐 2-个人网站',
  `link_name` varchar(50) NOT NULL COMMENT '网站名称',
  `link_url` varchar(100) NOT NULL COMMENT '网站链接',
  `link_description` varchar(100) NOT NULL COMMENT '网站描述',
  `link_rank` int NOT NULL DEFAULT '0' COMMENT '用于列表排序',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`link_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb3;

START TRANSACTION;

INSERT INTO `tb_admin_user` (`admin_user_id`, `login_user_name`, `login_password`, `nick_name`, `locked`) VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 'AshenOne', 0);
-- Rows for `tb_admin_user`: 1

INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (7, '关于慢写手记', 'about', '/admin/dist/img/rand/13.jpg', '## 写代码，也写生活\n\n慢写手记是一处安静的记录空间。这里放开发笔记、阅读摘记，也放一些与进度条无关的日常观察。\n\n“慢写”不是拖延，而是给一个问题足够的时间：先弄清楚它为什么发生，再把解决过程写得别人也能读懂。一篇不长但可以重现的笔记，往往比一大段没有边界的总结更有用。\n\n## 这里有什么\n\n- **开发实践**：顺着一个请求读代码，整理接口、数据和错误处理之间的关系。\n- **前端笔记**：从真实操作出发，关注表单、反馈和不同屏幕上的阅读体验。\n- **工程效率**：把修改过程、调试线索和写作方法留下来，减少重复摸索。\n- **日常随笔**：读书、散步、整理，以及那些值得停下来想一想的小事。\n\n技术文章会尽量写清问题、条件和验证方法。尚未整理好的内容先保存在草稿里，不急着发布。文章中的例子是理解问题的起点，使用时仍要结合自己的项目。\n\n## 从哪里开始\n\n可以先读[从一个请求开始，读懂博客项目](/read-a-request)，或者看看[一篇好用的 Markdown 笔记怎么写](/markdown-notes)。想换个话题时，也欢迎到日常随笔里坐一会儿。\n\n本站基于 [My Blog](https://github.com/ZHENFENG13/My-Blog) 搭建，使用 Spring Boot、Thymeleaf 和 MyBatis。感谢开源项目，让一个想法能够成为可以打开的页面。\n\n> 把今天弄明白的一点点，留给明天的自己。', 30, '关于本站', '本站,写作', 1, 15, 1, 0, '2026-10-01 02:00:00', '2026-10-01 02:00:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (8, '从一个请求开始，读懂博客项目', 'read-a-request', '/admin/dist/img/rand/13.jpg', '阅读一个陌生项目，最容易卡在“文件很多，但不知道从哪里开始”。与其先遍历每个目录，不如找一个能在页面上完成的小动作，顺着它走一遍。\n\n## 选一个足够小的入口\n\n以博客的文章列表为例：打开后台，看看标题、分类和状态是怎样出现的。这个动作有清楚的输入与输出，很适合作为阅读路线。\n\n在这个项目里，可以依次查看：\n\n1. `admin/blog.html`：页面提供了哪些按钮和表格容器？\n2. `admin/dist/js/blog.js`：表格向哪个地址请求数据，页面参数如何传递？\n3. `BlogController`：请求入口怎样校验并组织参数？\n4. `BlogServiceImpl`：列表和总数分别从哪里获取？\n5. `BlogMapper.xml`：查询如何筛选未删除的数据，分页条件如何拼接？\n\n这一圈走完后，先用自己的话写一句解释：“页面请求一页文章，控制器整理参数，服务查询列表和数量，再把结果返回给表格。”能写出这句话，说明各层之间的关系已经开始清楚了。\n\n## 观察三种结果\n\n只看正常列表，还不足以理解整个交互。可以再观察：没有符合条件的文章、参数不合法、请求没有成功返回。这三种结果的页面反馈应该能区分开。\n\n读代码时把问题记在旁边：\n\n- 空列表由谁决定？\n- 失败时按钮是否还能继续使用？\n- 前后端对字段长度的理解是否一致？\n\n## 最后再扩展\n\n列表看懂后，再去读添加、编辑与删除。每次只增加一个动作，之前画出的路线就会逐渐连成完整地图。\n\n这份地图不需要漂亮。写下入口、经过的文件、重要字段和一个实际结果，就足够帮助下一次定位问题。\n\n框架的通用概念可继续查阅 [Spring Boot 官方项目页](https://spring.io/projects/spring-boot/)；项目的具体行为以当前代码为准。', 26, '开发实践', 'Spring Boot,Java,工程实践', 1, 4, 0, 0, '2026-10-04 01:30:00', '2026-10-04 01:30:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (9, '把失败说清楚：接口响应的小约定', 'clear-api-responses', '/admin/dist/img/rand/19.jpg', '一次保存没有成功，可能是输入不符合要求，也可能是网络连接中断。用户看到的都像“没保存”，但接下来应该做的事情并不一样。\n\n## 把结果拆开看\n\n可以先为一次保存写下三种结果：\n\n| 结果 | 页面反馈 | 输入处理 |\n| --- | --- | --- |\n| 保存成功 | 告诉用户已经保存 | 以服务确认的内容为准 |\n| 业务校验失败 | 展示可以修正的原因 | 保留已经填写的内容 |\n| 请求异常 | 告诉用户本次未确认成功 | 保留内容，允许再次操作 |\n\n这个表格比一句“失败请重试”更能说明双方的约定。比如“标签数量限制为 6”，用户可以立即减少标签；“请求超时”，则需要先确认服务状态。\n\n## 消息要能够指导下一步\n\n“参数错误”通常不够具体。一个好的提示会指出哪个字段需要调整，并尽量说明限制，例如“文章标题不能超过 150 字”。\n\n同时，不要把服务器日志原样放进页面。页面需要的是可执行的提示，调试日志需要的是定位线索。它们服务于不同的读者。\n\n## 前后端一起验证\n\n前端校验可以及时减少无效提交，后端校验则负责保护实际写入。两处长度、必填项和状态约定应该一致。\n\n测试时可以刻意尝试：空标题、恰好达到长度限制、超过限制一个字符，以及保存失败后修改并重新提交。不要只检查是否弹出了消息，也要确认数据库有没有被部分修改。\n\n一份接口约定最终应回答三个问题：成功是什么样，失败是什么意思，用户还能怎样继续。', 26, '开发实践', '接口设计,Java,工程实践', 1, 0, 0, 0, '2026-10-05 02:15:00', '2026-10-05 02:15:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (10, '表单提交之后，按钮应该做什么', 'form-submit-states', '/admin/dist/img/rand/15.jpg', '用户点击保存后，最需要知道的是：页面是否收到了操作，现在进行到哪一步，是否还需要再点一次。\n\n## 给等待一个明确的状态\n\n一个表单至少可以区分准备输入、正在提交和提交结束三个阶段。\n\n进入提交阶段时，保存按钮可以暂时禁用，并把文字改为“保存中……”。这既能说明页面正在工作，也能减少重复点击。提交结束后，再根据结果恢复按钮和文字。\n\n这里容易漏掉的是失败路径。只在成功回调里恢复按钮，会让一次网络错误把整个表单留在不可操作的状态。\n\n## 失败以后，内容还应该在\n\n填完一篇长文章，提交失败，再看到一个空编辑器，是很难接受的体验。错误提示可以关闭，输入内容应该继续保留。\n\n本地草稿也要有清楚的边界：\n\n- 暂存到浏览器，并不等于已经保存到网站。\n- 恢复旧草稿之前，应让用户明确选择。\n- 保存请求进行期间继续输入的新内容，仍然属于未保存的修改。\n\n这些边界写清楚后，“已保存”的提示才值得相信。\n\n## 检查一次完整操作\n\n可以用同一张表单做三次验证：第一次正常保存，第二次制造一个校验失败，第三次在失败后修正并重试。每次都观察按钮、输入和结果提示。\n\n界面的可靠感，常常来自这些细节。用户不需要了解请求如何实现，只需要知道自己的内容还在，下一步也仍然可做。', 27, '前端笔记', 'JavaScript,表单交互', 1, 0, 0, 0, '2026-10-06 06:20:00', '2026-10-06 06:20:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (11, '小屏幕也要能读懂一张表', 'tables-on-small-screens', '/admin/dist/img/rand/22.jpg', '桌面端的一张表，可能同时包含标题、封面、状态、分类和时间。把它直接压进手机屏幕，结果往往是每列都很窄，长标题一行只能显示几个字。\n\n## 先问用户要看什么\n\n文章列表的主要任务是找到一篇文章，再进入编辑。标题应该先被看清，状态和分类帮助辨认，封面则提供视觉线索。\n\n如果这些内容必须同时保留，可以给表格一个合理的最小宽度，让外层容器横向滚动。这样小屏幕上的列宽仍然可读。\n\n```css\n.table-scroll {\n    max-width: 100%;\n    overflow-x: auto;\n}\n\n.article-table {\n    min-width: 760px;\n}\n```\n\n这只是布局示例，实际宽度仍要根据列内容和页面留白调整。有关 CSS 属性的具体行为，可以查阅 [MDN 的 CSS 参考](https://developer.mozilla.org/zh-CN/docs/Web/CSS)。\n\n## 让操作入口也能放得下\n\n窄屏下搜索框和按钮可以自然换行。比起把每个按钮都缩小，保留足够的点击区域通常更容易使用。\n\n侧栏收起以后，也要检查打开、关闭和键盘操作。菜单出现时，焦点应留在菜单中；关闭后，应回到能继续操作的位置。\n\n## 用实际内容检查\n\n短标题看起来很好，不代表长标题也可读。验证时放入一条较长的标题、一个有换行的描述和几条不同状态的数据，再观察是否出现意外遮挡。\n\n适配小屏幕，不只是让页面变窄。它需要重新检查信息的顺序，以及每一次点击之后发生的事情。', 27, '前端笔记', 'CSS,响应式', 1, 0, 0, 0, '2026-10-08 03:00:00', '2026-10-08 03:00:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (12, '给每次修改留一条清楚的记录', 'write-clear-commits', '/admin/dist/img/rand/13.jpg', '一段时间后回看修改记录，最有帮助的往往不是“优化代码”四个字，而是一句能让人想起问题的说明。\n\n## 先写发生了什么\n\n可以把一次修改写成三个部分：触发条件、原来的结果、修改后的结果。\n\n例如：\n\n> 分类编辑后再新增时，旧 ID 会留在表单里。新增现在会重置 ID，并根据表单状态选择保存入口。\n\n这句话说明了问题，也说明了行为。即使读者没有参加当时的讨论，也能知道为什么需要这个修改。\n\n## 把验证方法留下来\n\n“已测试”不如一条具体操作有用。可以写：“编辑一个分类，取消后点击新增，确认名称与 ID 已清空。”\n\n如果使用了自动化测试，也记录它覆盖的边界，例如请求失败后按钮能恢复、重复点击不会发出第二次请求。不要为了让记录显得完整，把没有执行过的验证也写进去。\n\n## 控制一次修改的大小\n\n当一条记录需要解释许多互不相关的问题时，可以先检查这些修改是否应该拆开。相关的文件一起提交，独立的问题分别记录，之后回溯会轻松很多。\n\n提交前可以查看当前差异，确认没有把本地配置、临时文件或调试输出混进去。Git 的具体命令和选项以 [官方参考文档](https://git-scm.com/docs) 为准。\n\n好的修改记录，是给以后留下一个入口。它让下一位读代码的人少猜一点，也让未来的自己少翻一点。', 28, '工程效率', 'Git,工程实践', 1, 0, 0, 0, '2026-10-08 08:30:00', '2026-10-08 08:30:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (13, '一篇好用的 Markdown 笔记怎么写', 'markdown-notes', '/admin/dist/img/rand/33.jpg', '一篇笔记能否在几个月后继续使用，通常取决于上下文是否完整，而不是标题是否足够醒目。\n\n## 先说清要解决的问题\n\n开头用两三句话写下：当时想做什么，遇到了什么现象，最后希望得到什么结果。这样读者在看到代码之前，就知道自己是否找对了文章。\n\n## 给内容一个稳定的顺序\n\n可以从这个提纲开始：\n\n```markdown\n## 问题\n## 条件与范围\n## 处理方法\n## 验证结果\n## 还没有解决的部分\n```\n\n不是每篇文章都需要全部保留，但“验证结果”很值得写。它可以是一条操作步骤、一段输出，或一个可以再次检查的现象。\n\n代码块外面补一句说明，告诉读者代码做什么、放在哪里。记录路径时，尽量使用项目中的相对位置，避免把个人机器的目录写成人人都能使用的条件。\n\n## 引用与自己的理解分开\n\n引用资料时留下链接，自己的推断则明确写出来。如果一个结论只在特定版本或场景中验证过，把这个范围也记下来。\n\nMarkdown 的标题、列表、链接和代码块已经足以组织大部分笔记。常见语法可查阅 [CommonMark 参考](https://commonmark.org/help/)。\n\n## 发布前再读一次\n\n试着把文章当作第一次看到：有没有跳过必要步骤，代码中的名字是否前后一致，链接是否还能打开？修掉这些小断点，笔记就更接近一份可以交给别人的说明。', 28, '工程效率', 'Markdown,写作', 1, 3, 0, 0, '2026-10-09 02:00:00', '2026-10-09 02:00:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (14, '周末，把待办清单变短一点', 'a-shorter-weekend-list', '/admin/dist/img/rand/33.jpg', '周末开始时，待办清单常常很长。整理房间、读几章书、补一篇笔记，再把搁置的项目推进一点。每件事都值得做，但放在同一天，休息也会变成赶进度。\n\n## 留下一件最想完成的事\n\n可以先问自己：如果今天只完成一件事，哪件最能让我觉得这一天过得踏实？\n\n它不一定是最重要的工作。也许只是把桌面清空，做一顿没有边看消息边吃的饭，或者认真读完一篇文章。\n\n选定以后，把其他事情写在“有余力再做”的区域。它们仍然存在，只是不再同时催促自己。\n\n## 给空白留一个位置\n\n散步的时候不必马上把念头整理成结论。看见光落在树叶上，听见街角有人说话，想起一本很久没翻的书，都可以只是发生过。\n\n有些想法需要过一会儿才知道是否值得记录。给它一点空白，比急着把每个片刻变成产出更舒服。\n\n## 晚上只记三句话\n\n- 今天完成了什么？\n- 什么事情让我轻松了一点？\n- 下次可以少安排什么？\n\n这样的记录不用写得漂亮。它帮助人看见一天真实的样子，而不是只盯着清单上剩下的方框。\n\n生活没有必要每次都交出一张满分答卷。能好好完成一件事，也能好好停下来，就已经是一种不错的节奏。', 29, '日常随笔', '生活,写作', 1, 1, 0, 0, '2026-10-10 01:00:00', '2026-10-10 01:00:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (15, '读完以后，留下一段自己的话', 'leave-your-own-words', '/admin/dist/img/rand/19.jpg', '读完一本书或一篇文章以后，很容易收藏链接、划几条线，然后继续打开下一篇。过一段时间再看，熟悉的是标题，留下的理解却很少。\n\n## 从一段自己的话开始\n\n合上页面，试着用几句话回答：这篇文章在讨论什么，它最有意思的地方在哪里，我还有什么没有想明白？\n\n不需要准确复述每一段。能用自己的词解释一个观点，比复制一页摘抄更容易暴露真正没有理解的部分。\n\n## 把问题写下来\n\n好的阅读记录不只有结论，也会留下问题。例如：\n\n- 这个观点适用于哪些情况？\n- 有没有一个反过来的例子？\n- 它与以前读到的内容有什么关系？\n\n这些问题可以暂时没有答案。下次遇到相关材料时，就知道该注意什么。\n\n## 再补一条行动\n\n如果内容与开发有关，可以找一个小例子试一试；如果与生活有关，可以留意一次实际经历。不要马上给自己安排一整套改造计划，一条小行动就够了。\n\n阅读笔记的价值，也许不在于留下了多少文字，而在于它让下一次阅读变得更有方向。\n\n书页可以合上，思考不必立刻结束。留下一段自己的话，就是让一件读过的事情继续生长。', 29, '日常随笔', '阅读,写作', 1, 3, 0, 0, '2026-10-11 07:00:00', '2026-10-11 07:00:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (16, '文章草稿：从问题到一篇可复用的笔记', 'draft-reusable-note', '/admin/dist/img/rand/13.jpg', '## 写作目标\n\n把一次解决问题的过程整理成其他人可以重复的笔记。这篇文章暂存为草稿，补齐实际例子后再发布。\n\n## 准备收集的材料\n\n- 问题最初出现时的页面或日志。\n- 能够再次触发问题的最短操作步骤。\n- 修改前后行为的区别。\n- 一个正常场景和一个边界场景的验证结果。\n\n## 文章结构\n\n1. 用简短文字描述问题，不预先宣布结论。\n2. 写清运行条件，说明哪些内容不在本次讨论范围。\n3. 保留决定性的代码片段，解释它为什么影响结果。\n4. 给出可重复的验证方式。\n5. 记录仍需进一步观察的地方。\n\n## 发布前检查\n\n检查步骤是否完整，引用是否有来源，截图中是否包含私人信息。删除只服务于调试的零散输出，保留能帮助读者理解的证据。', 28, '工程效率', '写作,工程实践', 0, 0, 0, 0, '2026-10-11 05:00:00', '2026-10-11 05:00:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (17, '开发随记：一次完整的故障复盘', 'draft-debug-review', '/admin/dist/img/rand/15.jpg', '## 这篇文章还需要一个真实案例\n\n先保留一份故障复盘提纲。遇到下一次问题时，可以直接沿着它收集信息，而不是在修复以后努力回忆当时发生了什么。\n\n## 现象\n\n记录哪个操作失败、出现的时间、影响范围，以及是否每次都能重现。\n\n## 定位过程\n\n按证据记录每一步：看到了什么，排除了什么，为什么决定继续检查下一处。猜测可以写，但要与已经验证的事实分开。\n\n## 原因与修复\n\n说明错误从哪里进入，又怎样影响到用户看到的结果。修复不仅要让当前例子恢复正常，也要解释相邻场景为何不会被破坏。\n\n## 验证\n\n- 原来的触发步骤已经通过。\n- 正常路径仍然可用。\n- 至少一个相关边界情况得到检查。\n- 记录中没有把未执行的测试写成通过。\n\n补齐这些材料后，再把草稿整理成一篇完整的文章。', 26, '开发实践', '调试,Java', 0, 0, 0, 0, '2026-10-11 06:00:00', '2026-10-11 06:00:00');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (18, 'aa', 'aaaa', '/admin/dist/img/rand/22.jpg', 'aaaaaaa', 26, '开发实践', 'a', 1, 8, 0, 0, '2026-10-11 09:19:06', '2026-10-11 09:19:06');
INSERT INTO `tb_blog` (`blog_id`, `blog_title`, `blog_sub_url`, `blog_cover_image`, `blog_content`, `blog_category_id`, `blog_category_name`, `blog_tags`, `blog_status`, `blog_views`, `enable_comment`, `is_deleted`, `create_time`, `update_time`) VALUES (19, '上传路径测试', '', 'http://localhost:8080/upload/20261011_18483037.jpg', '![上传目录测试](http://localhost:8080/upload/20261011_18474528.png "上传目录测试")', 26, '开发实践', '上传测试', 1, 1, 0, 1, '2026-10-11 10:48:35', '2026-10-11 10:48:35');
-- Rows for `tb_blog`: 13

INSERT INTO `tb_blog_category` (`category_id`, `category_name`, `category_icon`, `category_rank`, `is_deleted`, `create_time`) VALUES (26, '开发实践', '/admin/dist/img/category/13.png', 5, 0, '2026-10-11 09:01:15');
INSERT INTO `tb_blog_category` (`category_id`, `category_name`, `category_icon`, `category_rank`, `is_deleted`, `create_time`) VALUES (27, '前端笔记', '/admin/dist/img/category/12.png', 2, 0, '2026-10-11 09:01:15');
INSERT INTO `tb_blog_category` (`category_id`, `category_name`, `category_icon`, `category_rank`, `is_deleted`, `create_time`) VALUES (28, '工程效率', '/admin/dist/img/category/15.png', 3, 0, '2026-10-11 09:01:15');
INSERT INTO `tb_blog_category` (`category_id`, `category_name`, `category_icon`, `category_rank`, `is_deleted`, `create_time`) VALUES (29, '日常随笔', '/admin/dist/img/category/06.png', 2, 0, '2026-10-11 09:01:15');
INSERT INTO `tb_blog_category` (`category_id`, `category_name`, `category_icon`, `category_rank`, `is_deleted`, `create_time`) VALUES (30, '关于本站', '/admin/dist/img/category/09.png', 1, 0, '2026-10-11 09:01:15');
-- Rows for `tb_blog_category`: 5

INSERT INTO `tb_blog_comment` (`comment_id`, `blog_id`, `commentator`, `email`, `website_url`, `comment_body`, `comment_create_time`, `commentator_ip`, `reply_body`, `reply_create_time`, `comment_status`, `is_deleted`) VALUES (29, 18, 'aa', 'a@test.com', '', 'aaaa', '2026-10-11 09:30:51', '', '666', '2026-10-11 09:31:15', 1, 0);
INSERT INTO `tb_blog_comment` (`comment_id`, `blog_id`, `commentator`, `email`, `website_url`, `comment_body`, `comment_create_time`, `commentator_ip`, `reply_body`, `reply_create_time`, `comment_status`, `is_deleted`) VALUES (30, 18, 'test', 'a@test.com', '', 'test', '2026-10-11 09:41:17', '', '', '2026-10-11 09:41:17', 1, 0);
-- Rows for `tb_blog_comment`: 2

INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (136, '本站', 0, '2026-10-11 09:01:15');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (137, '写作', 0, '2026-10-11 09:01:15');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (138, 'Spring Boot', 0, '2026-10-11 09:01:15');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (139, 'Java', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (140, '工程实践', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (141, '接口设计', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (142, 'JavaScript', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (143, '表单交互', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (144, 'CSS', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (145, '响应式', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (146, 'Git', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (147, 'Markdown', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (148, '生活', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (149, '阅读', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (150, '调试', 0, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (151, 'a', 0, '2026-10-11 09:19:06');
INSERT INTO `tb_blog_tag` (`tag_id`, `tag_name`, `is_deleted`, `create_time`) VALUES (152, '上传测试', 0, '2026-10-11 10:48:35');
-- Rows for `tb_blog_tag`: 17

INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (283, 7, 136, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (284, 7, 137, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (285, 8, 138, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (286, 8, 139, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (287, 8, 140, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (288, 9, 141, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (289, 9, 139, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (290, 9, 140, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (291, 10, 142, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (292, 10, 143, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (293, 11, 144, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (294, 11, 145, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (295, 12, 146, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (296, 12, 140, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (297, 13, 147, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (298, 13, 137, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (299, 14, 148, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (300, 14, 137, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (301, 15, 149, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (302, 15, 137, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (303, 16, 137, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (304, 16, 140, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (305, 17, 150, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (306, 17, 139, '2026-10-11 09:01:16');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (307, 18, 151, '2026-10-11 09:19:06');
INSERT INTO `tb_blog_tag_relation` (`relation_id`, `blog_id`, `tag_id`, `create_time`) VALUES (308, 19, 152, '2026-10-11 10:48:35');
-- Rows for `tb_blog_tag_relation`: 26

INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('footerAbout', '写代码，也写生活。保持好奇，慢慢积累。', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('footerCopyRight', '© 2026 慢写手记', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('footerICP', '', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('footerPoweredBy', 'My Blog', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('footerPoweredByURL', 'https://github.com/wz-wsl/My-First-Blog', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('websiteDescription', '记录开发中的小问题，也记录生活里的小发现。把复杂的事情讲清楚，把值得留下的想法慢慢写下来。', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('websiteIcon', '/admin/dist/img/favicon.png', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('websiteLogo', '/admin/dist/img/logo2.png', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('websiteName', '慢写手记', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('yourAvatar', '/admin/dist/img/avatar.png', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('yourEmail', '', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
INSERT INTO `tb_config` (`config_name`, `config_value`, `create_time`, `update_time`) VALUES ('yourName', 'AshenOne', '2026-10-11 09:01:16', '2026-10-11 09:01:16');
-- Rows for `tb_config`: 12

INSERT INTO `tb_link` (`link_id`, `link_type`, `link_name`, `link_url`, `link_description`, `link_rank`, `is_deleted`, `create_time`) VALUES (20, 1, 'Spring Boot', 'https://spring.io/projects/spring-boot/', '官方项目介绍、使用指南与示例。', 1, 0, '2026-10-11 09:01:16');
INSERT INTO `tb_link` (`link_id`, `link_type`, `link_name`, `link_url`, `link_description`, `link_rank`, `is_deleted`, `create_time`) VALUES (21, 1, 'MDN Web Docs', 'https://developer.mozilla.org/zh-CN/', '查阅 HTML、CSS、JavaScript 与 Web API。', 2, 0, '2026-10-11 09:01:16');
INSERT INTO `tb_link` (`link_id`, `link_type`, `link_name`, `link_url`, `link_description`, `link_rank`, `is_deleted`, `create_time`) VALUES (22, 1, 'MyBatis 中文文档', 'https://mybatis.org/mybatis-3/zh_CN/', '持久层映射、配置与 SQL 映射参考。', 3, 0, '2026-10-11 09:01:16');
INSERT INTO `tb_link` (`link_id`, `link_type`, `link_name`, `link_url`, `link_description`, `link_rank`, `is_deleted`, `create_time`) VALUES (23, 1, 'Git 官方参考', 'https://git-scm.com/docs', '按命令查阅 Git 的用法与选项。', 4, 0, '2026-10-11 09:01:16');
INSERT INTO `tb_link` (`link_id`, `link_type`, `link_name`, `link_url`, `link_description`, `link_rank`, `is_deleted`, `create_time`) VALUES (24, 1, 'GitHub 中文文档', 'https://docs.github.com/zh', '仓库、拉取请求与协作流程的官方说明。', 5, 0, '2026-10-11 09:01:16');
INSERT INTO `tb_link` (`link_id`, `link_type`, `link_name`, `link_url`, `link_description`, `link_rank`, `is_deleted`, `create_time`) VALUES (25, 1, 'CommonMark', 'https://commonmark.org/help/', '用简短示例查看常见 Markdown 写法。', 6, 0, '2026-10-11 09:01:16');
-- Rows for `tb_link`: 6

COMMIT;
SET SQL_MODE=@OLD_SQL_MODE;
SET TIME_ZONE=@OLD_TIME_ZONE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
