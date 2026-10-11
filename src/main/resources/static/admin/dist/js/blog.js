$(function () {
    AdminUI.initGrid({
        url: '/admin/blogs/list', minWidth: 760,
        colModel: [
            {label: 'id', name: 'blogId', width: 50, key: true, hidden: true},
            {label: '标题', name: 'blogTitle', width: 180},
            {label: '预览图', name: 'blogCoverImage', width: 100, formatter: coverImageFormatter},
            {label: '浏览量', name: 'blogViews', width: 60},
            {label: '状态', name: 'blogStatus', width: 70, formatter: statusFormatter},
            {label: '博客分类', name: 'blogCategoryName', width: 100},
            {label: '添加时间', name: 'createTime', width: 130}
        ]
    });
    $('#keyword').on('keydown', function (event) {
        if (event.key === 'Enter') {
            search();
        }
    });
});

function coverImageFormatter(value) {
    return $('<img alt="" width="80" height="60">').attr('src', value)[0].outerHTML;
}

function statusFormatter(value) {
    return value == 1 ? '<span class="pill">已发布</span>' : '<span class="pill pill-warn">草稿</span>';
}

function search() {
    var keyword = $('#keyword').val().trim();
    if (!validLength(keyword, 20)) {
        swal('搜索关键字不能超过 20 字', {icon: 'warning'});
        return;
    }
    $('#jqGrid').jqGrid('setGridParam', {postData: {keyword: keyword}, page: 1}).trigger('reloadGrid');
}

function reload() {
    AdminUI.reloadGrid();
}

function addBlog() {
    window.location.href = '/admin/blogs/edit';
}

function editBlog() {
    var id = getSelectedRow();
    if (id != null) {
        window.location.href = '/admin/blogs/edit/' + id;
    }
}

function deleteBlog() {
    AdminUI.confirmBatch('/admin/blogs/delete', getSelectedRows(), '确认删除选中的文章吗？', '删除成功');
}
