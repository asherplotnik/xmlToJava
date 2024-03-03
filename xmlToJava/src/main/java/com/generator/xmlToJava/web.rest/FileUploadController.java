package com.generator.xmlToJava.web.rest;

import com.generator.xmlToJava.service.ProcessXMLAndGenerateZip;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final ProcessXMLAndGenerateZip processXMLService;

    @PostMapping("/upload")
    public ResponseEntity<Resource> uploadFile(@RequestParam("file") MultipartFile file) {
        String zipFilePath = processXMLService.processXMLAndGenerateZip(file);
        Resource fileResource = new FileSystemResource(zipFilePath);
        String contentDisposition = "attachment; filename=\"" + fileResource.getFilename() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(fileResource);
    }
}
