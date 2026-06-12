package com.github.lyd11250.bedrock.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.lyd11250.bedrock.common.Result;
import com.github.lyd11250.bedrock.system.service.FileService;
import com.github.lyd11250.bedrock.system.spi.FileBizTypeDef;
import com.github.lyd11250.bedrock.system.vo.FileVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件上传下载接口（基座通用能力）。下载为二进制流，不走统一 {@code Result} 包装。
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/v1/system/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping
    @SaCheckPermission("system:file:list")
    public Result<IPage<FileVO>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String bizType) {
        return Result.ok(fileService.page(current, size, bizType));
    }

    @GetMapping("/biz-types")
    @SaCheckPermission("system:file:list")
    public Result<List<FileBizTypeDef>> bizTypes() {
        return Result.ok(fileService.bizTypeOptions());
    }

    @PostMapping
    @SaCheckPermission("system:file:upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String bizType) {
        return Result.ok(fileService.upload(file, bizType));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:file:download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        FileService.DownloadFile df = fileService.download(id);
        MediaType mediaType = df.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(df.contentType());
        // RFC 5987：中文文件名走 filename*=UTF-8''<URL编码>，并保留 ASCII 回退
        String encoded = URLEncoder.encode(df.originalName(), StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType);
        if (df.size() >= 0) {
            builder.contentLength(df.size());
        }
        return builder.body(new InputStreamResource(df.content()));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:file:delete")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok();
    }
}
