// 用接口原始数据回填表单，不从格式化后的图片 HTML 中反推字段。
var categoryRows = {};

$(function () {
    AdminUI.initGrid({
        url: '/admin/categories/list', minWidth: 560,
        colModel: [
            {label: 'id', name: 'categoryId', width: 50, key: true, hidden: true},
            {label: '分类名称', name: 'categoryName', width: 240},
            {label: '分类图标', name: 'categoryIcon', width: 120, formatter: imgFormatter},
            {label: '添加时间', name: 'createTime', width: 120}
        ],
        loadComplete: function (result) {
            categoryRows = {};
            if (result && result.resultCode == 200 && result.data && Array.isArray(result.data.list)) {
                result.data.list.forEach(function (category) {
                    categoryRows[category.categoryId] = category;
                });
            }
        }
    });
    $('#categoryIcon').imagepicker({hide_select: false});
    $('#categoryForm').on('submit', function (event) {
        event.preventDefault();
        $('#saveButton').trigger('click');
    });
});

function imgFormatter(value) {
    return $('<img alt="分类图标" width="48" height="48">').attr('src', value)[0].outerHTML;
}

function reload() {
    AdminUI.reloadGrid();
}

function setCategoryIcon(value) {
    // change 事件同时同步 image-picker 的缩略图选中状态。
    $('#categoryIcon').val(value).trigger('change');
}

function reset() {
    $('#categoryId, #categoryName').val('');
    $('#edit-error-msg').hide().text('');
    setCategoryIcon($('#categoryIcon option:first').val());
}

function categoryAdd() {
    reset();
    $('#categoryModalLabel').text('新增分类');
    $('#categoryModal').modal('show');
}

function categoryEdit() {
    var id = getSelectedRow();
    if (id == null) {
        return;
    }
    var category = categoryRows[id];
    if (!category) {
        swal('分类数据已失效，请刷新列表后重试', {icon: 'warning'});
        return;
    }
    reset();
    $('#categoryId').val(id);
    $('#categoryName').val(category.categoryName);
    setCategoryIcon(category.categoryIcon);
    $('#categoryModalLabel').text('编辑分类');
    $('#categoryModal').modal('show');
}

$('#saveButton').on('click', function () {
    var name = $('#categoryName').val().trim();
    if (!validCN_ENString2_18(name)) {
        $('#edit-error-msg').show().text('分类名称需为 2–18 个中英文字符、数字或允许的分隔符');
        $('#categoryName').trigger('focus');
        return;
    }
    $('#categoryName').val(name);
    $('#edit-error-msg').hide().text('');
    // 弹窗 ID 决定保存模式，表格上勾选的旧行不会影响新增操作。
    var id = $('#categoryId').val();
    AdminUI.request({
        type: 'POST', url: id ? '/admin/categories/update' : '/admin/categories/save',
        data: $('#categoryForm').serialize(),
        success: function () {
            $('#categoryModal').modal('hide');
            swal('保存成功', {icon: 'success'});
            reload();
        }
    });
});

function deleteCagegory() {
    AdminUI.confirmBatch('/admin/categories/delete', getSelectedRows(), '确认删除选中的分类吗？', '删除成功');
}
