$(function () {
    AdminUI.initGrid({
        url: '/admin/tags/list', minWidth: 520,
        colModel: [
            {label: 'id', name: 'tagId', width: 50, key: true, hidden: true},
            {label: '标签名称', name: 'tagName', width: 240},
            {label: '添加时间', name: 'createTime', width: 120}
        ]
    });
});

function reload() {
    AdminUI.reloadGrid();
}

function tagAdd() {
    var name = $('#tagName').val().trim();
    if (!validCN_ENString2_18(name)) {
        swal('标签名称需为 2–18 个中英文字符、数字或允许的分隔符', {icon: 'warning'});
        return;
    }
    AdminUI.request({
        type: 'POST', url: '/admin/tags/save', data: {tagName: name},
        success: function () {
            $('#tagName').val('');
            swal('保存成功', {icon: 'success'});
            reload();
        }
    });
}

function deleteTag() {
    AdminUI.confirmBatch('/admin/tags/delete', getSelectedRows(), '确认删除选中的标签吗？', '删除成功');
}
