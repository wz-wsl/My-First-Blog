$(function () {
    // 卡片独立提交，按钮禁用与恢复由公共请求函数成对处理。
    function bindSave(button, form, url) {
        $(button).on('click', function () {
            AdminUI.request({
                type: 'POST', url: url, data: $(form).serialize(),
                buttons: $(button), busyText: '保存中…',
                success: function (result) {
                    if (result.data === false) {
                        swal('未保存任何设置，请检查输入后重试', {icon: 'warning'});
                        return;
                    }
                    swal('保存成功', {icon: 'success'});
                }
            });
        });
    }
    bindSave('#updateWebsiteButton', '#websiteForm', '/admin/configurations/website');
    bindSave('#updateUserInfoButton', '#userInfoForm', '/admin/configurations/userInfo');
    bindSave('#updateFooterButton', '#footerForm', '/admin/configurations/footer');
});
