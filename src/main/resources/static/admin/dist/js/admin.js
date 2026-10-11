/**
 * 后台公共脚本（替代原来的 adminlte.min.js）
 * 只负责界面交互，不涉及任何数据请求：
 *   1. 顶部栏显示今天的日期
 *   2. 小屏幕下打开 / 关闭侧边栏
 */
(function () {
    var WEEK_DAYS = ['日', '一', '二', '三', '四', '五', '六'];

    // 补零：3 → "03"
    function pad(n) {
        return n < 10 ? '0' + n : String(n);
    }

    // 1. 填充日期，格式：2026 · 10 · 11 · 星期日
    function renderToday() {
        var now = new Date();
        var text = now.getFullYear() + ' · ' + pad(now.getMonth() + 1) + ' · ' + pad(now.getDate())
            + ' · 星期' + WEEK_DAYS[now.getDay()];
        document.querySelectorAll('[data-today]').forEach(function (el) {
            el.textContent = text;
        });
    }

    // 2. 侧边栏开关：给 body 加 / 去 sidebar-open 类，样式见 layout.css
    function setupSidebarToggle() {
        var body = document.body;

        // 遮罩层：点击后关闭侧边栏
        var mask = document.createElement('div');
        mask.className = 'sidebar-mask';
        mask.addEventListener('click', function () {
            body.classList.remove('sidebar-open');
        });
        body.appendChild(mask);

        document.querySelectorAll('[data-toggle-sidebar]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                body.classList.toggle('sidebar-open');
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        renderToday();
        setupSidebarToggle();
    });
})();
