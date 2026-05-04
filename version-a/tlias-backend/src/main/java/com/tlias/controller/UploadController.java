package com.tlias.controller;

import com.tlias.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
public class UploadController {

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result upload(MultipartFile file) throws IOException {
        log.info("文件上传：{}", file.getOriginalFilename());

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 生成唯一文件名
        String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + extName;

        // 保存文件
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        file.transferTo(new File(dir, newFileName));

        return Result.success("/uploads/" + newFileName);
    }
}
