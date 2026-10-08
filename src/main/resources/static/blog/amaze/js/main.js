// 前台公共交互：导航展开、搜索、回到顶部、验证码刷新、代码高亮
(function () {
    var navToggle = document.querySelector('.nav-toggle');
    var navLinks = document.getElementById('nav-links');
    if (navToggle && navLinks) {
        navToggle.addEventListener('click', function () {
            var open = navLinks.classList.toggle('is-open');
            navToggle.setAttribute('aria-expanded', String(open));
        });
    }

    // 搜索走 /search/{keyword}，关键字需要编码
    var searchForm = document.querySelector('.search-form');
    if (searchForm) {
        searchForm.addEventListener('submit', function (e) {
            e.preventDefault();
            var keyword = searchForm.elements.keyword.value.trim();
            if (keyword) {
                window.location.href = searchForm.getAttribute('action') + encodeURIComponent(keyword);
            }
        });
    }

    var backTop = document.querySelector('.back-top');
    if (backTop) {
        window.addEventListener('scroll', function () {
            backTop.hidden = window.scrollY < 300;
        }, { passive: true });
    }

    var captcha = document.querySelector('.captcha-refresh');
    if (captcha) {
        captcha.addEventListener('click', function () {
            var img = captcha.querySelector('img');
            img.src = img.getAttribute('data-src') + '?d=' + Date.now();
        });
    }

    if (window.hljs) {
        document.querySelectorAll('.article-content pre code').forEach(function (block) {
            window.hljs.highlightBlock(block);
        });
    }

    // 标题里的锚点链接在当前页跳转
    document.querySelectorAll('.article-content :is(h1, h2, h3, h4, h5) a').forEach(function (a) {
        a.removeAttribute('target');
    });
})();
