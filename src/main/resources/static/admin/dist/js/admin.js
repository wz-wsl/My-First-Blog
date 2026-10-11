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
        var sidebar = document.getElementById('adminSidebar');
        var main = document.querySelector('.admin-main');
        var toggle = document.querySelector('[data-toggle-sidebar]');
        if (!sidebar || !main || !toggle) {
            return;
        }
        var mobile = window.matchMedia('(max-width: 991.98px)');
        var originalOverflow = body.style.overflow;
        var mask = document.createElement('div');
        mask.className = 'sidebar-mask';
        body.appendChild(mask);

        function setOpen(open, restoreFocus) {
            body.classList.toggle('sidebar-open', open);
            toggle.setAttribute('aria-expanded', String(open));
            toggle.setAttribute('aria-label', open ? '关闭菜单' : '打开菜单');
            main.inert = open;
            body.style.overflow = open ? 'hidden' : originalOverflow;
            if (!open && (restoreFocus || (mobile.matches && sidebar.contains(document.activeElement)))) {
                toggle.focus();
            }
            sidebar.setAttribute('aria-hidden', String(mobile.matches && !open));
            sidebar.inert = mobile.matches && !open;
            if (open) {
                sidebar.querySelector('a').focus();
            }
        }

        toggle.addEventListener('click', function () {
            setOpen(!body.classList.contains('sidebar-open'), true);
        });
        mask.addEventListener('click', function () { setOpen(false, true); });
        // 抽屉打开时焦点留在菜单内；关闭后回到开关，避免键盘落到遮罩后面。
        document.addEventListener('keydown', function (event) {
            if (!body.classList.contains('sidebar-open')) {
                return;
            }
            if (event.key === 'Escape') {
                event.preventDefault();
                setOpen(false, true);
            } else if (event.key === 'Tab') {
                var links = sidebar.querySelectorAll('a[href], button:not([disabled])');
                var first = links[0];
                var last = links[links.length - 1];
                if (event.shiftKey && document.activeElement === first) {
                    event.preventDefault();
                    last.focus();
                } else if (!event.shiftKey && document.activeElement === last) {
                    event.preventDefault();
                    first.focus();
                }
            }
        });
        mobile.addEventListener('change', function () { setOpen(false, false); });
        setOpen(false, false);
    }

    document.addEventListener('DOMContentLoaded', function () {
        renderToday();
        setupSidebarToggle();
    });
})();
