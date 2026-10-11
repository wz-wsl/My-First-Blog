/**
 * 概览页脚本
 *   1. 根据当前时间显示「上午好 / 下午好 / 晚上好」
 *   2. 读取最近 4 篇文章和最近 3 条评论
 *
 * 数据来自后台已有的列表接口（和文章管理、评论管理页面用的是同一个），只读取，不修改任何数据。
 * 评论内容是访客填写的，所以统一用 text() 写入页面，避免把 HTML 当成代码执行。
 */
$(function () {
    renderGreeting();
    loadRecentPosts();
    loadRecentComments();
});

/** 问候语 */
function renderGreeting() {
    var hour = new Date().getHours();
    var text = hour < 6 ? '夜深了' : hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好';
    $('[data-greeting]').text(text);
}

/**
 * 请求列表接口的第一页
 * @param url      接口地址
 * @param limit    取几条
 * @param onList   拿到数据后的回调，参数是数组
 * @param $box     数据填充的容器，用来显示「暂无」或「加载失败」
 */
function fetchFirstPage(url, limit, onList, $box) {
    $.ajax({
        type: 'GET',
        url: url,
        data: {page: 1, limit: limit},
        success: function (result) {
            var list = result && result.resultCode == 200 && result.data ? result.data.list : null;
            if (list && list.length) {
                $box.empty();
                onList(list);
            } else {
                $box.html('<p class="empty-tip">暂无内容</p>');
            }
        },
        error: function () {
            $box.html('<p class="empty-tip">加载失败，请刷新重试</p>');
        }
    });
}

/** 最近文章：封面 + 标题 + 分类日期 + 状态 + 阅读量，点击进入编辑 */
function loadRecentPosts() {
    var $box = $('#recentPosts');
    fetchFirstPage('/admin/blogs/list', 4, function (blogs) {
        blogs.forEach(function (blog) {
            var isPublished = blog.blogStatus == 1;
            var $row = $('<a class="post-row"></a>').attr('href', '/admin/blogs/edit/' + blog.blogId);

            $('<img class="post-cover" alt="">').attr('src', blog.blogCoverImage).appendTo($row);

            $('<div></div>')
                .append($('<h3 class="post-title"></h3>').text(blog.blogTitle))
                .append($('<p class="post-meta"></p>').text(blog.blogCategoryName + ' · ' + formatDate(blog.createTime)))
                .appendTo($row);

            $('<div class="post-side"></div>')
                .append($('<span class="pill"></span>')
                    .toggleClass('pill-warn', !isPublished)
                    .text(isPublished ? '已发布' : '草稿'))
                .append($('<div class="post-views"></div>').text((blog.blogViews || 0) + ' 阅'))
                .appendTo($row);

            $box.append($row);
        });
    }, $box);
}

/** 最新评论：评论人 + 状态 + 时间 + 内容 */
function loadRecentComments() {
    var $box = $('#recentComments');
    fetchFirstPage('/admin/comments/list', 3, function (comments) {
        comments.forEach(function (comment) {
            var isChecked = comment.commentStatus == 1;
            var $head = $('<div class="comment-head"></div>')
                .append($('<b></b>').text(comment.commentator))
                .append($('<span class="pill"></span>')
                    .toggleClass('pill-warn', !isChecked)
                    .text(isChecked ? '已审核' : '待审核'))
                .append($('<span class="comment-time"></span>').text(formatDate(comment.commentCreateTime)));

            $('<div class="comment-item"></div>')
                .append($head)
                .append($('<p class="comment-body"></p>').text(comment.commentBody))
                .appendTo($box);
        });
    }, $box);
}

/** "2019-04-25 04:46:15" → "2019-04-25"，只保留日期部分 */
function formatDate(dateTime) {
    return dateTime ? String(dateTime).substring(0, 10) : '';
}
