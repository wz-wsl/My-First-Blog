const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test} = require('node:test');

const root = path.resolve(__dirname, '../../..');
const admin = 'src/main/resources/static/admin/dist/js/';
const front = 'src/main/resources/static/blog/plugins/comment/';

// 模拟表单和请求边界，不依赖服务或数据库，也不会发出真实写入请求。
function harness() {
    const nodes = new Map();
    const ready = [];
    const requests = [];
    const alerts = [];
    const listeners = {};
    const storage = new Map();
    const node = key => {
        if (!nodes.has(key)) {
            nodes.set(key, {value: '', disabled: false, innerHTML: '保存', attrs: {}, events: {}, modal: null});
        }
        return nodes.get(key);
    };
    function $(selector) {
        if (typeof selector === 'function') {
            ready.push(selector);
            return;
        }
        let keys = typeof selector === 'string' ? selector.split(',').map(value => value.trim()) : [];
        if (selector === 'input[name="blogStatus"]:checked') {
            keys = [node('#publish').checked ? '#publish' : '#draft'];
        }
        if (selector === 'input[name="enableComment"]:checked') {
            keys = [node('#enableCommentTrue').checked ? '#enableCommentTrue' : '#enableCommentFalse'];
        }
        const elements = keys.map(node);
        const api = {
            length: elements.length,
            each(fn) { elements.forEach((element, index) => fn.call(element, index, element)); return api; },
            val(value) {
                if (value === undefined) return elements[0]?.value;
                elements.forEach(element => { element.value = value; }); return api;
            },
            prop(name, value) {
                if (value === undefined) return elements[0]?.[name];
                elements.forEach(element => { element[name] = value; }); return api;
            },
            attr(name, value) {
                if (value === undefined) return elements[0]?.attrs[name];
                elements.forEach(element => { element.attrs[name] = value; }); return api;
            },
            text(value) { return api.html(value); },
            empty() { elements.forEach(element => { element.children = []; element.innerHTML = ''; }); return api; },
            append(child) { elements.forEach(element => { (element.children ||= []).push(child); }); return api; },
            appendTo(parent) { parent.append(api); return api; },
            html(value) {
                if (value === undefined) return elements[0]?.innerHTML;
                elements.forEach(element => { element.innerHTML = value; }); return api;
            },
            filter() { return {length: elements.filter(element => element.disabled).length}; },
            on(events, fn) {
                elements.forEach(element => events.split(' ').forEach(event => { element.events[event] = fn; }));
                return api;
            },
            trigger(event) { elements.forEach(element => element.events[event]?.()); return api; },
            hide() { return api.prop('hidden', true); },
            show() { return api.prop('hidden', false); },
            modal(value) { return api.prop('modal', value); },
            imagepicker() { return api; },
            importTags(value) { return api.val(value); },
            serialize() {
                return new URLSearchParams({categoryId: node('#categoryId').value, categoryName: node('#categoryName').value}).toString();
            }
        };
        elements.forEach((element, index) => { api[index] = element; });
        return api;
    }
    $.extend = Object.assign;
    $.ajax = settings => { requests.push(settings); return {}; };
    const context = vm.createContext({
        $, console, setTimeout, clearTimeout,
        swal: (...args) => { alerts.push(args); return Promise.resolve(true); },
        window: {location: {}, addEventListener: (event, fn) => { listeners[event] = fn; }},
        localStorage: {
            getItem: key => storage.get(key) || null,
            setItem: (key, value) => storage.set(key, value),
            removeItem: key => storage.delete(key)
        }
    });
    const load = file => vm.runInContext(fs.readFileSync(path.join(root, file), 'utf8'), context, {filename: file});
    const finish = (settings, result) => { settings.success(result); settings.complete(); };
    return {context, load, node, ready, requests, alerts, listeners, storage, finish};
}

test('评论允许标点、表情和换行，200 字通过，201 字拒绝', () => {
    const h = harness();
    h.load(front + 'valid.js');
    for (const body of ['写得很好！', '请问怎么配置？', '第一行\n第二行', '谢谢😀', 'a'.repeat(200)]) {
        assert.equal(h.context.validCommentBody(body), true);
    }
    for (const body of ['', ' \n ', 'a'.repeat(201), null]) {
        assert.equal(h.context.validCommentBody(body), false);
    }
});

test('后台回复采用相同长度规则，通用长度校验包含上限', () => {
    const h = harness();
    h.load(admin + 'public.js');
    assert.equal(h.context.validLength('a'.repeat(150), 150), true);
    assert.equal(h.context.validLength('a'.repeat(151), 150), false);
    assert.equal(h.context.validCommentBody('好的！\n谢谢😀'), true);
    assert.equal(h.context.validCommentBody('a'.repeat(201)), false);
});

test('分类编辑回填原数据，随后新增不受旧选中行和 ID 影响', () => {
    const h = harness();
    h.load(admin + 'public.js');
    let grid;
    h.context.AdminUI.initGrid = options => { grid = options; };
    h.context.getSelectedRow = () => '24';
    h.node('#categoryIcon option:first').value = '/default.png';
    h.load(admin + 'category.js');
    h.ready.shift()();
    grid.loadComplete({resultCode: 200, data: {list: [{categoryId: 24, categoryName: '日常随笔', categoryIcon: '/original.png'}]}});
    h.context.categoryEdit();
    assert.equal(h.node('#categoryName').value, '日常随笔');
    assert.equal(h.node('#categoryIcon').value, '/original.png');
    assert.equal(h.node('#categoryId').value, '24');
    h.context.categoryAdd();
    assert.equal(h.node('#categoryId').value, '');
    assert.equal(h.node('#categoryIcon').value, '/default.png');
    h.node('#categoryName').value = '新增分类';
    h.node('#saveButton').events.click();
    assert.equal(h.requests[0].url, '/admin/categories/save');
    assert.equal(new URLSearchParams(h.requests[0].data).get('categoryId'), '');
    h.finish(h.requests[0], {resultCode: 500, message: '名称重复'});
    assert.equal(h.node('#categoryModal').modal, 'show');
    assert.equal(h.node('#categoryName').value, '新增分类');
    assert.equal(h.node('#saveButton').disabled, false);
});

test('请求期间只提交一次，业务失败和网络失败后都可重试', () => {
    const h = harness();
    h.load(admin + 'public.js');
    let successes = 0;
    const options = {type: 'POST', url: '/save', buttons: h.context.$('#button'), busyText: '保存中…', success: () => { successes++; }};
    h.context.AdminUI.request(options);
    h.context.AdminUI.request(options);
    assert.equal(h.requests.length, 1);
    assert.equal(h.node('#button').disabled, true);
    h.finish(h.requests[0], {resultCode: 500, message: '失败'});
    assert.equal(successes, 0);
    assert.equal(h.node('#button').disabled, false);
    assert.equal(h.node('#button').innerHTML, '保存');
    h.context.AdminUI.request(options);
    h.requests[1].error({status: 0}, 'timeout');
    h.requests[1].complete();
    assert.equal(h.node('#button').disabled, false);
    h.context.AdminUI.request(options);
    h.finish(h.requests[2], {resultCode: 200});
    assert.equal(successes, 1);
});

test('三张配置卡片成功或失败后都恢复自己的保存按钮', () => {
    const h = harness();
    h.load(admin + 'public.js');
    h.load(admin + 'configuration.js');
    h.ready.shift()();
    for (const button of ['#updateWebsiteButton', '#updateUserInfoButton', '#updateFooterButton']) {
        h.node(button).events.click();
        assert.equal(h.node(button).disabled, true);
        h.finish(h.requests.at(-1), {resultCode: 200, data: true});
        assert.equal(h.node(button).disabled, false);
        h.node(button).events.click();
        h.finish(h.requests.at(-1), {resultCode: 500, message: '保存失败'});
        assert.equal(h.node(button).disabled, false);
    }
});

function editorHarness() {
    const h = harness();
    h.load(admin + 'public.js');
    h.load(admin + 'edit.js');
    h.node('#blogName').value = '标题';
    h.node('#blogSubUrl').value = '';
    h.node('#blogCategoryId').value = '24';
    h.node('#blogTags').value = '标签';
    h.node('#blogId').value = '4';
    h.node('#blogForm').attrs['data-draft-owner'] = '1';
    h.node('#blogCoverImage').attrs.src = '/cover.png';
    h.node('#draft').value = '0';
    h.node('#publish').value = '1';
    h.node('#enableCommentTrue').checked = true;
    h.node('#enableCommentTrue').value = '0';
    h.context.blogEditor = {content: '正文', getMarkdown() { return this.content; }, setMarkdown(value) { this.content = value; }};
    return h;
}

test('正文长度实际检查正文，而非短标签；上限允许提交', () => {
    const h = editorHarness();
    const draft = h.context.articleSnapshot();
    draft.content = 'a'.repeat(100000);
    assert.equal(h.context.validateArticle(draft, true), true);
    draft.content += 'a';
    assert.equal(h.context.validateArticle(draft, true), false);
});

test('本地草稿按账号与文章隔离，恢复时不自动覆盖服务器内容', () => {
    const h = editorHarness();
    const draft = h.context.articleSnapshot();
    draft.content = '本地未保存内容';
    h.storage.set('my-blog:draft:v1:1:4', JSON.stringify(draft));
    h.context.initializeDraftProtection();
    assert.equal(h.context.blogEditor.content, '正文');
    assert.equal(h.node('#draftRecovery').hidden, false);
    h.node('#restoreDraftButton').events.click();
    assert.equal(h.context.blogEditor.content, '本地未保存内容');
    assert.equal(h.node('#draftRecovery').hidden, true);
    const event = {preventDefault() { this.prevented = true; }};
    h.listeners.beforeunload(event);
    assert.equal(event.prevented, true);
    assert.equal(h.context.draftKey, 'my-blog:draft:v1:1:4');
});

test('保存成功仅清理已提交内容，请求期间的新编辑继续暂存', () => {
    const h = editorHarness();
    h.context.initializeDraftProtection();
    h.context.blogEditor.content = '已提交版本';
    h.node('#saveButton').events.click();
    const request = h.requests[0];
    assert.equal(request.data.blogContent, '已提交版本');
    h.context.blogEditor.content = '请求期间继续编辑';
    h.finish(request, {resultCode: 200});
    assert.equal(JSON.parse(h.storage.get(h.context.draftKey)).content, '请求期间继续编辑');
});

test('前台评论防重复提交，失败保留正文，成功清空正文并恢复按钮', () => {
    const h = harness();
    h.load(front + 'valid.js');
    h.load(front + 'comment.js');
    for (const [field, value] of Object.entries({blogId: '4', commentator: '访客', email: 'test@example.com', websiteUrl: '', verifyCode: 'test', commentBody: '写得很好！\n谢谢'})) {
        h.node('#' + field).value = value;
    }
    h.node('#email').validity = {valid: true};
    const click = h.node('#commentSubmit').events.click;
    click();
    click();
    assert.equal(h.requests.length, 1);
    h.finish(h.requests[0], {resultCode: 500, message: '验证码错误'});
    assert.equal(h.node('#commentBody').value, '写得很好！\n谢谢');
    assert.equal(h.node('#commentSubmit').disabled, false);
    click();
    h.finish(h.requests[1], {resultCode: 200, data: true});
    assert.equal(h.node('#commentBody').value, '');
    assert.equal(h.node('#verifyCode').value, '');
    assert.equal(h.node('#commentSubmit').disabled, false);
});

test('概览业务失败提供重试，不伪装成空数据', () => {
    const h = harness();
    h.load(admin + 'dashboard.js');
    const box = h.context.$('#recentPosts');
    h.context.fetchFirstPage('/list', 4, () => { throw new Error('失败不应渲染列表'); }, box);
    h.requests[0].success({resultCode: 500, message: '加载失败'});
    assert.equal(h.node('#recentPosts').children[0][0].innerHTML, '加载失败');
    const retry = h.node('#recentPosts').children[1][0].events.click;
    retry();
    assert.equal(h.requests.length, 2);
    h.requests[1].success({resultCode: 200, data: {list: []}});
    assert.match(h.node('#recentPosts').innerHTML, /暂无内容/);
});

test('成功保存全部修改后清理本地草稿且不再提示未保存', () => {
    const h = editorHarness();
    h.context.initializeDraftProtection();
    h.context.blogEditor.content = '新版本';
    h.context.saveLocalDraft();
    assert.equal(h.storage.has(h.context.draftKey), true);
    h.node('#saveButton').events.click();
    h.finish(h.requests[0], {resultCode: 200});
    assert.equal(h.storage.has(h.context.draftKey), false);
    const event = {preventDefault() { this.prevented = true; }};
    h.listeners.beforeunload(event);
    assert.equal(event.prevented, undefined);
});

test('本地存储不可用时仍保护未保存内容，并提示及时保存', () => {
    const h = editorHarness();
    h.context.initializeDraftProtection();
    h.context.blogEditor.content = '尚未保存';
    h.context.localStorage.setItem = () => { throw new Error('quota exceeded'); };
    assert.doesNotThrow(() => h.context.saveLocalDraft());
    assert.match(h.node('#draftStatus').innerHTML, /暂存失败/);
    const event = {preventDefault() { this.prevented = true; }};
    h.listeners.beforeunload(event);
    assert.equal(event.prevented, true);
});
