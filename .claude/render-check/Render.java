import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Throwaway: render admin templates with mock data to verify they parse and to preview them. */
public class Render {
    public static void main(String[] args) throws Exception {
        String templates = args[0];
        File out = new File(args[1]);
        out.mkdirs();

        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(templates + "/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setLinkBuilder(new StandardLinkBuilder() {
            @Override
            protected String computeContextPath(IExpressionContext c, String base, Map<String, Object> p) {
                return "";
            }
        });

        Map<String, Object> session = new HashMap<>();
        session.put("loginUser", "十三");
        session.put("errorMsg", "验证码错误");

        Map<String, Object> blog = new HashMap<>();
        blog.put("blogId", 7);
        blog.put("blogTitle", "程序员，你怎么这么忙？");
        blog.put("blogSubUrl", "busy");
        blog.put("blogTags", "随笔,生活");
        blog.put("blogCategoryId", 2);
        blog.put("blogContent", "## hello\n正文");
        blog.put("blogCoverImage", "/admin/dist/img/rand/1.jpg");
        blog.put("blogStatus", 1);
        blog.put("enableComment", 0);

        List<Map<String, Object>> cats = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> c = new HashMap<>();
            c.put("categoryId", i);
            c.put("categoryName", new String[]{"日常随笔", "SSM 整合进阶篇", "Spring Boot"}[i - 1]);
            cats.add(c);
        }
        Map<String, String> conf = new HashMap<>();
        conf.put("websiteName", "My Blog");
        conf.put("yourName", "十三");
        conf.put("footerICP", "浙ICP备 xxxxxx 号");

        String[][] pages = {
                {"admin/index", "index"}, {"admin/blog", "blogs"}, {"admin/comment", "comments"},
                {"admin/category", "categories"}, {"admin/tag", "tags"}, {"admin/link", "links"},
                {"admin/edit", "edit"}, {"admin/profile", "profile"}, {"admin/configuration", "configurations"},
                {"admin/test", "test"}, {"admin/login", ""}
        };
        for (String[] p : pages) {
            Context ctx = new Context(Locale.CHINA);
            ctx.setVariable("session", session);
            ctx.setVariable("path", p[1]);
            ctx.setVariable("blogCount", 49);
            ctx.setVariable("commentCount", 11);
            ctx.setVariable("categoryCount", 10);
            ctx.setVariable("tagCount", 72);
            ctx.setVariable("linkCount", 16);
            ctx.setVariable("categories", cats);
            ctx.setVariable("configurations", conf);
            ctx.setVariable("loginUserName", "admin");
            ctx.setVariable("nickName", "十三");
            if (p[0].endsWith("edit")) ctx.setVariable("blog", blog);
            String html = engine.process(p[0], ctx);
            String name = p[0].substring(6) + ".html";
            Files.write(new File(out, name).toPath(), html.getBytes(StandardCharsets.UTF_8));
            System.out.println("OK " + name);
        }
    }
}
