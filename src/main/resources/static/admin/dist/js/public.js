/**
 * 判空
 *
 * @param obj
 * @returns {boolean}
 */
function isNull(obj) {
    if (obj == null || obj == undefined || obj.trim() == "") {
        return true;
    }
    return false;
}

/**
 * 参数长度验证
 *
 * @param obj
 * @param length
 * @returns {boolean}
 */
function validLength(obj, length) {
    // 最大长度包含边界；是否必填由调用方单独判断。
    return obj != null && String(obj).trim().length <= length;
}

/** 正文允许标点、表情和换行，不复用名称的字符白名单。 */
function validCommentBody(value) {
    return !isNull(value) && validLength(value, 200);
}

/**
 * url验证
 *
 * @param str
 * @returns {boolean}
 */
function isURL(str_url) {
    var strRegex = "^((https|http|ftp|rtsp|mms)?://)"
        + "(([0-9]{1,3}\.){3}[0-9]{1,3}"
        + "|"
        + "([0-9a-zA-Z_!~*'()-]+\.)*"
        + "([0-9a-zA-Z][0-9a-zA-Z-]{0,61})?[0-9a-zA-Z]\."
        + "[a-zA-Z]{2,6})"
        + "(:[0-9]{1,4})?"
        + "((/?)|"
        + "(/[0-9a-zA-Z_!~*'().;?:@&=+$,%#-]+)+/?)$";
    var re = new RegExp(strRegex);
    if (re.test(str_url)) {
        return (true);
    } else {
        return (false);
    }
}

/**
 * 用户名称验证 4到16位（字母，数字，下划线，减号）
 *
 * @param userName
 * @returns {boolean}
 */
function validUserName(userName) {
    var pattern = /^[a-zA-Z0-9_-]{4,16}$/;
    if (pattern.test(userName.trim())) {
        return (true);
    } else {
        return (false);
    }
}

/**
 * 正则匹配2-18位的中英文字符串
 *
 * @param str
 * @returns {boolean}
 */
function validCN_ENString2_18(str) {
    var pattern = /^[a-zA-Z0-9-\u4E00-\u9FA5_,， ]{2,18}$/;
    if (pattern.test(str.trim())) {
        return (true);
    } else {
        return (false);
    }
}

/**
 * 正则匹配2-100位的中英文字符串
 *
 * @param str
 * @returns {boolean}
 */
function validCN_ENString2_100(str) {
    var pattern = /^[a-zA-Z0-9-\u4E00-\u9FA5_,， ]{2,100}$/;
    if (pattern.test(str.trim())) {
        return (true);
    } else {
        return (false);
    }
}

/**
 * 用户密码验证 最少6位，最多20位字母或数字、特殊字符的组合
 *
 * @param password
 * @returns {boolean}
 */
function validPassword(password) {
    var pattern = /^(?![0-9]+$)(?![a-z]+$)(?![A-Z]+$)(?!([^(0-9a-zA-Z)])+$)^.{6,20}$/;
    if (pattern.test(password.trim())) {
        return (true);
    } else {
        return (false);
    }
}


/**
 * 获取jqGrid选中的一条记录
 * @returns {*}
 */
function getSelectedRow() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if (!rowKey) {
        swal("请选择一条记录", {
            icon: "warning",
        });
        return;
    }
    var selectedIDs = grid.getGridParam("selarrrow");
    if (selectedIDs.length > 1) {
        swal("只能选择一条记录", {
            icon: "warning",
        });
        return;
    }
    return selectedIDs[0];
}

/**
 * 获取jqGrid选中的一条记录(不出现弹框)
 * @returns {*}
 */
function getSelectedRowWithoutAlert() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if (!rowKey) {
        return;
    }
    var selectedIDs = grid.getGridParam("selarrrow");
    if (selectedIDs.length > 1) {
        return;
    }
    return selectedIDs[0];
}

/**
 * 获取jqGrid选中的多条记录
 * @returns {*}
 */
function getSelectedRows() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if (!rowKey) {
        swal("请选择一条记录", {
            icon: "warning",
        });
        return;
    }
    return grid.getGridParam("selarrrow");
}

/** 共用请求与表格行为；页面只提供业务字段和成功后的操作。 */
var AdminUI = (function () {
    var pending = {};

    function showError(message) {
        swal(message || '操作失败，请重试', {icon: 'error'});
    }

    function request(options) {
        var key = (options.type || 'GET') + ':' + options.url;
        if (pending[key]) {
            return; // 请求未结束时不再次发送，避免快速连点重复提交。
        }
        var $buttons = options.buttons || $('.toolbar button, .page-actions button, #saveButton');
        if ($buttons.length && $buttons.filter(':disabled').length === $buttons.length) {
            return;
        }
        var states = [];
        $buttons.each(function () {
            states.push({element: this, disabled: this.disabled, html: this.innerHTML});
        });
        $buttons.prop('disabled', true);
        if (options.busyText) {
            $buttons.text(options.busyText);
        }
        pending[key] = true;
        var settings = $.extend({}, options, {
            dataType: 'json',
            timeout: options.timeout || 30000,
            success: function (result) {
                if (!result || result.resultCode != 200) {
                    showError(result && result.message);
                    return; // 失败不关闭表单，保留输入供用户修正。
                }
                if (options.success) {
                    options.success(result);
                }
            },
            error: function (xhr, status) {
                var message = xhr.status === 401 || xhr.status === 403 || status === 'parsererror'
                    ? '登录可能已失效，请重新登录后重试'
                    : status === 'timeout' ? '请求超时，请重试' : '网络请求失败，请重试';
                showError(message);
            },
            complete: function () {
                delete pending[key];
                states.forEach(function (state) {
                    state.element.disabled = state.disabled;
                    state.element.innerHTML = state.html;
                });
                if (options.complete) {
                    options.complete();
                }
            }
        });
        delete settings.buttons;
        delete settings.busyText;
        return $.ajax(settings);
    }

    function initGrid(options) {
        var $grid = $('#jqGrid');
        var minWidth = options.minWidth || 720;
        var $feedback = $('<p class="grid-feedback" role="status" aria-live="polite" hidden></p>');
        var loadComplete = options.loadComplete;
        var settings = $.extend({
            datatype: 'json', height: 'auto', rowNum: 10, rowList: [10, 20, 50],
            styleUI: 'Bootstrap', loadtext: '信息读取中…', autowidth: true,
            multiselect: true, pager: '#jqGridPager',
            jsonReader: {root: 'data.list', page: 'data.currPage', total: 'data.totalPage', records: 'data.totalCount'},
            prmNames: {page: 'page', rows: 'limit', order: 'order'},
            ajaxGridOptions: {dataType: 'json', timeout: 30000}
        }, options, {
            loadComplete: function (result) {
                var valid = result && result.resultCode == 200 && result.data && Array.isArray(result.data.list);
                $feedback.prop('hidden', !!valid).text(valid ? '' : (result && result.message) || '加载失败，请重新加载页面');
                if (loadComplete) {
                    loadComplete(result);
                }
            },
            loadError: function () {
                $feedback.prop('hidden', false).text('加载失败，请检查网络或重新登录后重试');
            }
        });
        delete settings.minWidth;
        $grid.jqGrid(settings);
        // 列宽保持可读，窄屏由外层滚动容器承接溢出。
        $grid.closest('.ui-jqgrid').wrap('<div class="grid-scroll" tabindex="0" role="region" aria-label="数据表格，可横向滚动"></div>');
        var $scroll = $grid.closest('.grid-scroll');
        $feedback.insertBefore($scroll);
        function resize() {
            $grid.jqGrid('setGridWidth', Math.max(minWidth, $scroll.width()));
        }
        $(window).on('resize.adminGrid', resize);
        resize();
    }

    function reloadGrid() {
        $('#jqGrid').trigger('reloadGrid');
    }

    function confirmBatch(url, ids, message, successMessage) {
        if (!ids || !ids.length) {
            return;
        }
        swal({title: '确认操作', text: message, icon: 'warning', buttons: ['取消', '确认'], dangerMode: true})
            .then(function (confirmed) {
                if (confirmed) {
                    request({
                        type: 'POST', url: url, contentType: 'application/json', data: JSON.stringify(ids),
                        success: function () {
                            swal(successMessage, {icon: 'success'});
                            reloadGrid();
                        }
                    });
                }
            });
    }

    return {request: request, initGrid: initGrid, reloadGrid: reloadGrid, confirmBatch: confirmBatch};
})();
