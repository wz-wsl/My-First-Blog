var commentSubmitting = false;

$('#commentSubmit').on('click', function () {
    if (commentSubmitting) {
        return;
    }
    var data = {
        blogId: $('#blogId').val(),
        verifyCode: $('#verifyCode').val().trim(),
        commentator: $('#commentator').val().trim(),
        email: $('#email').val().trim(),
        websiteUrl: $('#websiteUrl').val().trim(),
        commentBody: $('#commentBody').val().trim()
    };
    if (isNull(data.blogId)) {
        swal('参数异常，请刷新页面', {icon: 'warning'});
        return;
    }
    if (!validCN_ENString2_100(data.commentator)) {
        swal('请输入 2–100 字的中英文称呼', {icon: 'warning'});
        return;
    }
    if (isNull(data.email) || !$('#email')[0].validity.valid) {
        swal('请输入正确的邮箱地址', {icon: 'warning'});
        return;
    }
    if (!validCommentBody(data.commentBody)) {
        swal('评论不能为空，且不能超过 200 字', {icon: 'warning'});
        return;
    }
    if (isNull(data.verifyCode)) {
        swal('请输入验证码', {icon: 'warning'});
        return;
    }
    commentSubmitting = true;
    var $button = $('#commentSubmit');
    var originalLabel = $button.html();
    $button.prop('disabled', true).text('提交中…');
    $.ajax({
        type: 'POST', url: '/blog/comment', dataType: 'json', timeout: 30000, data: data,
        success: function (result) {
            if (result && result.resultCode == 200 && result.data !== false) {
                swal('评论提交成功，请等待博主审核', {icon: 'success'});
                $('#commentBody, #verifyCode').val('');
                $('.captcha-refresh').trigger('click');
            } else {
                // 业务失败时保留正文，避免用户重新输入。
                swal(result && result.message || '评论提交失败，请重试', {icon: 'error'});
            }
        },
        error: function () {
            swal('请求失败，请检查网络后重试', {icon: 'error'});
        },
        complete: function () {
            commentSubmitting = false;
            $button.prop('disabled', false).html(originalLabel);
        }
    });
});
