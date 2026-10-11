$(function () {
    AdminUI.initGrid({
        url: '/admin/links/list', minWidth: 820,
        colModel: [
            {label: 'id', name: 'linkId', width: 50, key: true, hidden: true},
            {label: '网站名称', name: 'linkName', width: 100},
            {label: '网站链接', name: 'linkUrl', width: 160},
            {label: '网站描述', name: 'linkDescription', width: 180},
            {label: '排序值', name: 'linkRank', width: 60},
            {label: '添加时间', name: 'createTime', width: 130}
        ]
    });
    $('#linkForm').on('submit', function (event) {
        event.preventDefault();
        $('#saveButton').trigger('click');
    });
});

function reload() {
    AdminUI.reloadGrid();
}

function linkAdd() {
    reset();
    $('#linkModal .modal-title').text('新增友链');
    $('#linkModal').modal('show');
}

$('#saveButton').on('click', function () {
    var name = $('#linkName').val();
    var url = $('#linkUrl').val();
    var description = $('#linkDescription').val();
    var rank = $('#linkRank').val();
    var error = !validCN_ENString2_18(name) ? '请输入 2–18 字的规范网站名称'
        : !isURL(url) ? '请输入正确的网站地址'
        : isNull(description) || !validLength(description, 100) ? '网站描述不能为空且不能超过 100 字'
        : isNull(rank) || !Number.isInteger(Number(rank)) || Number(rank) < 0 ? '排序值需为非负整数'
        : '';
    if (error) {
        $('#edit-error-msg').show().text(error);
        return;
    }
    $('#edit-error-msg').hide().text('');
    var id = $('#linkId').val();
    AdminUI.request({
        type: 'POST', url: id > 0 ? '/admin/links/update' : '/admin/links/save',
        data: $('#linkForm').serialize(),
        success: function (result) {
            if (result.data === false) {
                swal('保存失败，请检查输入后重试', {icon: 'error'});
                return;
            }
            $('#linkModal').modal('hide');
            swal('保存成功', {icon: 'success'});
            reload();
        }
    });
});

function linkEdit() {
    var id = getSelectedRow();
    if (id == null) {
        return;
    }
    // 数据取得并回填后再展示，避免用户保存尚未加载完成的空表单。
    AdminUI.request({
        type: 'GET', url: '/admin/links/info/' + id,
        success: function (result) {
            if (!result.data) {
                swal('友链数据已失效，请刷新列表后重试', {icon: 'warning'});
                return;
            }
            reset();
            $('#linkId').val(id);
            $('#linkName').val(result.data.linkName);
            $('#linkUrl').val(result.data.linkUrl);
            $('#linkDescription').val(result.data.linkDescription);
            $('#linkRank').val(result.data.linkRank);
            $('#linkType').val(result.data.linkType);
            $('#linkModal .modal-title').text('编辑友链');
            $('#linkModal').modal('show');
        }
    });
}

function deleteLink() {
    AdminUI.confirmBatch('/admin/links/delete', getSelectedRows(), '确认删除选中的友链吗？', '删除成功');
}

function reset() {
    $('#linkId').val(0);
    $('#linkName, #linkUrl, #linkDescription').val('');
    $('#linkRank').val(0);
    $('#linkType').val($('#linkType option:first').val());
    $('#edit-error-msg').hide().text('');
}
