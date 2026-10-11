var replyCommentId = null;
var commentRows = {};

$(function () {
    AdminUI.initGrid({
        url: '/admin/comments/list', minWidth: 900,
        colModel: [
            {label: 'id', name: 'commentId', width: 50, key: true, hidden: true},
            {label: '评论内容', name: 'commentBody', width: 180},
            {label: '评论时间', name: 'commentCreateTime', width: 130},
            {label: '评论人名称', name: 'commentator', width: 90},
            {label: '评论人邮箱', name: 'email', width: 150},
            {label: '状态', name: 'commentStatus', width: 80, formatter: statusFormatter},
            {label: '回复内容', name: 'replyBody', width: 180}
        ],
        loadComplete: function (result) {
            commentRows = {};
            if (result && result.resultCode == 200 && result.data && Array.isArray(result.data.list)) {
                result.data.list.forEach(function (comment) {
                    commentRows[comment.commentId] = comment;
                });
            }
        }
    });
    $('#replyForm').on('submit', function (event) {
        event.preventDefault();
        $('#saveButton').trigger('click');
    });
});

function statusFormatter(value) {
    return value == 1 ? '<span class="pill">已审核</span>' : '<span class="pill pill-warn">待审核</span>';
}

function reload() {
    AdminUI.reloadGrid();
}

function checkDoneComments() {
    AdminUI.confirmBatch('/admin/comments/checkDone', getSelectedRows(), '确认审核通过选中的评论吗？', '审核成功');
}

function deleteComments() {
    AdminUI.confirmBatch('/admin/comments/delete', getSelectedRows(), '确认删除选中的评论吗？', '删除成功');
}

function reply() {
    var id = getSelectedRow();
    if (id == null) {
        return;
    }
    var comment = commentRows[id];
    if (!comment || comment.commentStatus != 1) {
        swal('请先审核该评论再进行回复', {icon: 'warning'});
        return;
    }
    // 弹窗打开后固定回复目标，不再依赖随后可能变化的表格选中行。
    replyCommentId = id;
    $('#replyBody').val(comment.replyBody || '');
    $('#replyModal').modal('show');
}

$('#saveButton').on('click', function () {
    var body = $('#replyBody').val().trim();
    if (!validCommentBody(body)) {
        swal('回复不能为空，且不能超过 200 字', {icon: 'warning'});
        return;
    }
    if (replyCommentId == null) {
        return;
    }
    AdminUI.request({
        type: 'POST', url: '/admin/comments/reply',
        data: {commentId: replyCommentId, replyBody: body},
        success: function () {
            $('#replyModal').modal('hide');
            swal('回复成功', {icon: 'success'});
            reload();
        }
    });
});
