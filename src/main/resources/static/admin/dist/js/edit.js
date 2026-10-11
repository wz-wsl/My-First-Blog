var blogEditor;
var draftReady = false;
var draftBaseline;
var draftKey;
var draftTimer;
var recoverableDraft;

$(function () {
    $('#blogTags').tagsInput({width: '100%', height: '38px', defaultText: '文章标签', onChange: scheduleDraftSave});
    $('.select2').select2();
    blogEditor = editormd('blog-editormd', {
        width: '100%', height: 640, syncScrolling: 'single',
        path: '/admin/plugins/editormd/lib/', toolbarModes: 'full',
        imageUpload: true, imageFormats: ['jpg', 'jpeg', 'gif', 'png', 'bmp', 'webp'],
        imageUploadURL: '/admin/blogs/md/uploadfile',
        onload: function () {
            blogEditor = this;
            initializeDraftProtection();
        },
        onchange: scheduleDraftSave
    });

    // 一次粘贴可能包含多张图，各自上传，不使用表单的防重复提交锁。
    document.getElementById('blog-editormd').addEventListener('paste', function (event) {
        var items = event.clipboardData && event.clipboardData.items;
        if (!items) {
            return;
        }
        Array.from(items).forEach(function (item) {
            if (item.type.indexOf('image/') !== 0) {
                return;
            }
            var file = item.getAsFile();
            if (!file) {
                return;
            }
            var data = new FormData();
            data.append('file', file);
            $.ajax({
                type: 'POST', url: '/admin/upload/file', data: data,
                processData: false, contentType: false, dataType: 'json', timeout: 30000,
                success: function (result) {
                    if (result && result.resultCode == 200) {
                        blogEditor.insertValue('![](' + result.data + ')');
                    } else {
                        swal(result && result.message || '图片上传失败，请重试', {icon: 'error'});
                    }
                },
                error: function () {
                    swal('图片上传失败，请重试', {icon: 'error'});
                }
            });
        });
    });
    new AjaxUpload('#uploadCoverImage', {
        action: '/admin/upload/file', name: 'file', autoSubmit: true, responseType: 'json',
        onSubmit: function (file, extension) {
            if (!extension || !/^(jpg|jpeg|png|gif)$/i.test(extension)) {
                swal('只支持 jpg、jpeg、png、gif 格式的封面', {icon: 'warning'});
                return false;
            }
        },
        onComplete: function (file, result) {
            if (result && result.resultCode == 200) {
                setCoverImage(result.data);
            } else {
                swal(result && result.message || '封面上传失败，请重试', {icon: 'error'});
            }
        }
    });
});

function setCoverImage(url) {
    $('#blogCoverImage').attr('src', url).attr('style', 'width:128px;height:96px;');
    scheduleDraftSave();
}

/** 两个保存入口共用校验，弹窗打开后改动字段也不会绕过最终检查。 */
function validateArticle(draft, requireCover) {
    var error = isNull(draft.title) ? '请输入文章标题'
        : !validLength(draft.title, 150) ? '标题不能超过 150 字'
        : !validLength(draft.path, 150) ? '路径不能超过 150 字'
        : isNull(draft.category) ? '请选择文章分类'
        : isNull(draft.tags) ? '请输入文章标签'
        : !validLength(draft.tags, 150) ? '标签不能超过 150 字'
        : isNull(draft.content) ? '请输入文章内容'
        : !validLength(draft.content, 100000) ? '文章内容不能超过 100000 字'
        : requireCover && (isNull(draft.cover) || draft.cover.indexOf('img-upload') !== -1) ? '请选择封面图片'
        : '';
    if (error) {
        swal(error, {icon: 'warning'});
    }
    return !error;
}

$('#confirmButton').on('click', function () {
    if (!draftReady) {
        swal('编辑器正在加载，请稍后再试', {icon: 'info'});
        return;
    }
    if (validateArticle(articleSnapshot(), false)) {
        updateSaveLabel();
        $('#articleModal').modal('show');
    }
});

$('#saveButton').on('click', function () {
    if (!draftReady) {
        return;
    }
    var draft = articleSnapshot();
    if (!validateArticle(draft, true)) {
        return;
    }
    var blogId = $('#blogId').val();
    var data = {
        blogTitle: draft.title, blogSubUrl: draft.path, blogCategoryId: draft.category,
        blogTags: draft.tags, blogContent: draft.content, blogCoverImage: draft.cover,
        blogStatus: draft.status, enableComment: draft.comments
    };
    if (blogId > 0) {
        data.blogId = blogId;
    }
    AdminUI.request({
        type: 'POST', url: blogId > 0 ? '/admin/blogs/update' : '/admin/blogs/save', data: data,
        success: function () {
            // 只把提交时的快照标为已保存，请求期间继续写的内容仍保留为草稿。
            draftBaseline = JSON.stringify(draft);
            recoverableDraft = null;
            $('#draftRecovery').prop('hidden', true);
            saveLocalDraft();
            $('#articleModal').modal('hide');
            swal({
                title: draft.status == 1 ? '文章已发布' : '草稿已保存',
                icon: 'success', button: '返回文章列表',
                closeOnEsc: false, closeOnClickOutside: false
            }).then(function () {
                window.location.href = '/admin/blogs';
            });
        }
    });
});

$('#cancelButton').on('click', function () {
    window.location.href = '/admin/blogs';
});

$('#randomCoverImage').on('click', function () {
    setCoverImage('/admin/dist/img/rand/' + (Math.floor(Math.random() * 40) + 1) + '.jpg');
});

/** 快照只保存当前文章的编辑字段，按账号与文章 ID 隔离本地草稿。 */
function articleSnapshot() {
    return {
        title: $('#blogName').val(), path: $('#blogSubUrl').val(),
        category: $('#blogCategoryId').val(), tags: $('#blogTags').val(),
        content: blogEditor.getMarkdown(), cover: $('#blogCoverImage').attr('src'),
        status: $('input[name="blogStatus"]:checked').val(),
        comments: $('input[name="enableComment"]:checked').val()
    };
}

function saveLocalDraft() {
    if (!draftReady) {
        return;
    }
    var draft = articleSnapshot();
    var changed = JSON.stringify(draft) !== draftBaseline;
    try {
        if (changed) {
            localStorage.setItem(draftKey, JSON.stringify(draft));
            $('#draftStatus').text('本地草稿已暂存，修改尚未保存到网站');
        } else if (!recoverableDraft) {
            localStorage.removeItem(draftKey);
            $('#draftStatus').text('当前没有未保存的修改');
        }
    } catch (error) {
        $('#draftStatus').text('本地草稿暂存失败，请及时保存到网站');
    }
}

function scheduleDraftSave() {
    if (!draftReady) {
        return;
    }
    clearTimeout(draftTimer);
    draftTimer = setTimeout(saveLocalDraft, 500);
}

function initializeDraftProtection() {
    draftBaseline = JSON.stringify(articleSnapshot());
    draftKey = 'my-blog:draft:v1:' + $('#blogForm').attr('data-draft-owner') + ':' + $('#blogId').val();
    draftReady = true;
    $('#draftStatus').text('当前没有未保存的修改');
    try {
        var saved = localStorage.getItem(draftKey);
        var parsed = saved ? JSON.parse(saved) : null;
        if (parsed && typeof parsed.content === 'string' && JSON.stringify(parsed) !== draftBaseline) {
            recoverableDraft = parsed;
            $('#draftRecovery').prop('hidden', false);
        }
    } catch (error) {
        $('#draftStatus').text('无法读取本地草稿，请及时保存到网站');
    }
    $('#blogForm, #articleModal').on('input change', scheduleDraftSave);
    $('#restoreDraftButton').on('click', function () {
        if (!recoverableDraft) {
            return;
        }
        var draft = recoverableDraft;
        $('#blogName').val(draft.title);
        $('#blogSubUrl').val(draft.path);
        $('#blogCategoryId').val(draft.category).trigger('change');
        $('#blogTags').importTags(draft.tags || '');
        blogEditor.setMarkdown(draft.content);
        $('#blogCoverImage').attr('src', draft.cover);
        $('#publish').prop('checked', draft.status == 1);
        $('#draft').prop('checked', draft.status != 1);
        $('#enableCommentTrue').prop('checked', draft.comments == 0);
        $('#enableCommentFalse').prop('checked', draft.comments != 0);
        recoverableDraft = null;
        $('#draftRecovery').prop('hidden', true);
        updateSaveLabel();
        saveLocalDraft();
    });
    $('#discardDraftButton').on('click', function () {
        recoverableDraft = null;
        $('#draftRecovery').prop('hidden', true);
        saveLocalDraft();
    });
    // 刷新、返回列表和关闭页面统一使用浏览器的离开确认；失败时仍保留草稿。
    window.addEventListener('beforeunload', function (event) {
        if (JSON.stringify(articleSnapshot()) !== draftBaseline) {
            saveLocalDraft();
            event.preventDefault();
            event.returnValue = '';
        }
    });
    updateSaveLabel();
}

function updateSaveLabel() {
    $('#saveButton').text($('#publish').prop('checked') ? '发布文章' : '保存草稿');
}

$('input[name="blogStatus"]').on('change', updateSaveLabel);
