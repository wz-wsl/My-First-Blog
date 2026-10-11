"""Throwaway preview server: serves rendered admin pages + real static assets + mock list APIs."""
import json, os, sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

ROOT = os.path.dirname(os.path.abspath(__file__))
STATIC = sys.argv[1]
OUT = os.path.join(ROOT, 'out')

PAGES = {'/admin/index': 'index', '/admin/blogs': 'blog', '/admin/comments': 'comment',
         '/admin/categories': 'category', '/admin/tags': 'tag', '/admin/links': 'link',
         '/admin/blogs/edit': 'edit', '/admin/profile': 'profile', '/admin/configurations': 'configuration',
         '/admin/login': 'login', '/admin/test': 'test'}

def page(rows):
    return {'resultCode': 200, 'message': 'SUCCESS',
            'data': {'totalCount': len(rows), 'pageSize': 10, 'totalPage': 1, 'currPage': 1, 'list': rows}}

BLOGS = [
    {'blogId': 50, 'blogTitle': '关于秋天的一些碎碎念', 'blogCoverImage': '/admin/dist/img/rand/3.jpg', 'blogViews': 0,
     'blogStatus': 0, 'blogCategoryName': '日常随笔', 'createTime': '2026-10-11 14:20:00'},
    {'blogId': 49, 'blogTitle': 'test', 'blogCoverImage': '/admin/dist/img/rand/1.jpg', 'blogViews': 11,
     'blogStatus': 1, 'blogCategoryName': '日常随笔', 'createTime': '2019-05-07 11:49:06'},
    {'blogId': 48, 'blogTitle': 'Spring+SpringMVC+MyBatis整合(easyUI、AdminLte3)', 'blogCoverImage': '/admin/dist/img/rand/5.jpg',
     'blogViews': 43, 'blogStatus': 1, 'blogCategoryName': 'SSM整合进阶篇', 'createTime': '2019-04-25 04:46:15'},
    {'blogId': 47, 'blogTitle': '文章总目录', 'blogCoverImage': '/admin/dist/img/rand/8.jpg', 'blogViews': 9,
     'blogStatus': 1, 'blogCategoryName': '日常随笔', 'createTime': '2019-04-25 04:42:23'},
]
COMMENTS = [
    {'commentId': 3, 'commentBody': '写得太真实了，周末也在改 bug 的我默默点赞。<b>xss?</b>', 'commentCreateTime': '2026-10-11 16:30:00',
     'commentator': '小林', 'email': 'lin@example.com', 'commentStatus': 0, 'replyBody': ''},
    {'commentId': 2, 'commentBody': '请问 SSM 系列还会继续更新吗？', 'commentCreateTime': '2026-10-11 15:12:00',
     'commentator': 'Echo', 'email': 'echo@example.com', 'commentStatus': 0, 'replyBody': ''},
    {'commentId': 1, 'commentBody': '收藏了，感谢分享', 'commentCreateTime': '2026-10-09 09:00:00',
     'commentator': '路人甲', 'email': 'a@example.com', 'commentStatus': 1, 'replyBody': '谢谢支持～'},
]
CATS = [{'categoryId': i, 'categoryName': n, 'categoryIcon': '/admin/dist/img/category/%02d.png' % i,
         'createTime': '2019-04-20 10:00:00'} for i, n in enumerate(['日常随笔', 'SSM整合进阶篇', 'Spring Boot'], 1)]
TAGS = [{'tagId': i, 'tagName': n, 'createTime': '2019-04-20 10:00:00'} for i, n in enumerate(['java', 'spring', '随笔', 'mybatis'], 1)]
LINKS = [{'linkId': 1, 'linkType': 0, 'linkName': '十三的博客', 'linkUrl': 'http://13blog.site', 'linkDescription': '个人独立博客',
          'linkRank': 1, 'createTime': '2019-04-20 10:00:00'}]
APIS = {'/admin/blogs/list': BLOGS, '/admin/comments/list': COMMENTS, '/admin/categories/list': CATS,
        '/admin/tags/list': TAGS, '/admin/links/list': LINKS}


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=STATIC, **kw)

    def log_message(self, *a):
        pass

    def send_bytes(self, body, ctype):
        self.send_response(200)
        self.send_header('Content-Type', ctype)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        path = urlparse(self.path).path.rstrip('/') or '/admin/index'
        if path.startswith('/admin/blogs/edit'):
            path = '/admin/blogs/edit'
        if path in APIS:
            return self.send_bytes(json.dumps(page(APIS[path]), ensure_ascii=False).encode(), 'application/json; charset=utf-8')
        if path in PAGES:
            with open(os.path.join(OUT, PAGES[path] + '.html'), 'rb') as f:
                return self.send_bytes(f.read(), 'text/html; charset=utf-8')
        if path == '/common/kaptcha':
            self.path = '/admin/dist/img/logo.png'
        return super().do_GET()


ThreadingHTTPServer(('127.0.0.1', int(sys.argv[2])), Handler).serve_forever()
