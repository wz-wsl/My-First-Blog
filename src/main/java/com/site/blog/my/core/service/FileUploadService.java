package com.site.blog.my.core.service;

import com.site.blog.my.core.util.MyBlogUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 后台图片上传：封面上传和 Markdown 编辑器上传共用。
 */
@Service
public class FileUploadService {

    /** 只允许图片，html、svg 等可以执行脚本的文件一律拒绝 */
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path uploadDir;

    public FileUploadService(@Value("${config.FILE_UPLOAD_DIC}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 保存上传的图片，返回可以直接访问的完整地址。
     *
     * @throws IllegalArgumentException 文件为空或类型不允许，message 可直接展示给用户
     * @throws IOException              写入磁盘失败
     */
    public String storeImage(MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("只支持上传 jpg、jpeg、png、gif、webp 格式的图片");
        }
        // UUID 文件名不会重复，也不会带上用户提供的路径字符
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Files.createDirectories(uploadDir);
        file.transferTo(uploadDir.resolve(newFileName));
        return buildUrl(request, newFileName);
    }

    private static String buildUrl(HttpServletRequest request, String fileName) {
        try {
            URI host = MyBlogUtils.getHost(new URI(request.getRequestURL().toString()));
            return (host == null ? "" : host.toString()) + "/upload/" + fileName;
        } catch (URISyntaxException e) {
            return "/upload/" + fileName;
        }
    }
}
