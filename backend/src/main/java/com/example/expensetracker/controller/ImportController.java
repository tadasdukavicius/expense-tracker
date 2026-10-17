package com.example.expensetracker.controller;

import com.example.expensetracker.dto.ImportPreviewResponse;
import com.example.expensetracker.service.ImportService;
import lombok.RequiredArgsConstructor;
import com.example.expensetracker.dto.ImportResultResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportPreviewResponse preview(
            @RequestParam Long accountId,
            @RequestPart("file") MultipartFile file) {
        return importService.preview(accountId, file);
    }

    @PostMapping("/{id}/confirm")
    public ImportResultResponse confirm(@PathVariable Long id) {
        return importService.confirm(id);
    }
}