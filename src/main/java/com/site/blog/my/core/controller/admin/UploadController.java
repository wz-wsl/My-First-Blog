package com.site.blog.my.core.controller.admin;

import com.site.blog.my.core.service.FileUploadService;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@Controller
@RequestMapping("/admin")
public class UploadController {

    @Resource
    private FileUploadService fileUploadService;

    @PostMapping({"/upload/file"})
    @ResponseBody
    public Result upload(HttpServletRequest httpServletRequest, @RequestParam("file") MultipartFile file) {
        try {
            Result resultSuccess = ResultGenerator.genSuccessResult();
            resultSuccess.setData(fileUploadService.storeImage(file, httpServletRequest));
            return resultSuccess;
        } catch (IllegalArgumentException e) {
            return ResultGenerator.genFailResult(e.getMessage());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResultGenerator.genFailResult("文件上传失败");
        }
    }

}
