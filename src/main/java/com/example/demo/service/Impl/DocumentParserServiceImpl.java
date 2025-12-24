package com.example.demo.service.Impl;

import com.example.demo.service.DocumentParserService;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserServiceImpl.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md"
    );

    private final Tika tika = new Tika();


    @Override
    public String parseDocument(MultipartFile file) {
        if(file == null || file.isEmpty()){
            throw new IllegalArgumentException("文件不能为空");
        }

        try(InputStream inputStream = file.getInputStream()){
            // Tika自动识别文件类型并提取文本
            String text = tika.parseToString(inputStream);
            log.info("成功解析文档：{},提取文本长度：{}",file.getOriginalFilename(),text.length());
            return text;
        } catch (IOException | TikaException e) {
            log.error("解析文档失败：{}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档解析失败：" + e.getMessage(), e);
        }
    }

    @Override
    public Mono<String> parseDocumentReactive(FilePart filePart) {
        if (filePart == null) {
            return Mono.error(new IllegalArgumentException("文件不能为空"));
        }

        String filename = filePart.filename();
        if (filename.isEmpty()) {
            return Mono.error(new IllegalArgumentException("文件名不能为空"));
        }

        // 使用 DataBufferUtils 读取文件内容为字节数组
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .map(bytes -> {
                    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                        String text = tika.parseToString(inputStream);
                        log.info("成功解析文档：{},提取文本长度：{}", filename, text.length());
                        return text;
                    } catch (IOException | TikaException e) {
                        log.error("解析文档失败：{}", filename, e);
                        throw new RuntimeException("文档解析失败：" + e.getMessage(), e);
                    }
                });
    }

    @Override
    public boolean isSupportedFileType(String filename) {
        if(filename == null){
            return false;
        }
        String extension = getFileExtension(filename).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    @Override
    public String getFileType(String filename) {
        return getFileExtension(filename).toLowerCase();
    }

    //截取文件后缀名
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}
