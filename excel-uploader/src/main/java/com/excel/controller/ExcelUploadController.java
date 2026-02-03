package com.excel.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.excel.entity.FileUploadLog;
import com.excel.repository.FileUploadLogRepository;
import com.excel.dto.UploadAnalyticsDTO;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/excel")
@CrossOrigin(origins = "http://localhost:3000")
public class ExcelUploadController {

    @Value("${excel.read.path}")
    private String INPUT_PATH;

    @Autowired
    private FileUploadLogRepository logRepo;

    /* =====================================================
       FILE UPLOAD (ONLY QUEUE)
       ===================================================== */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            File dir = new File(INPUT_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            LocalDateTime now = LocalDateTime.now();
            String user = "SYSTEM";

            FileUploadLog log = new FileUploadLog();
            log.setFilename(file.getOriginalFilename());

            /* ========= STATE 1: QUEUED ========= */
            log.setStatus("QUEUED");
            log.setValidationPoint("QUEUED"); // ✅ NOT NULL + LOGICALLY CORRECT
            log.setErrorMessage("File queued for processing");

            log.setLogDate(now);
            log.setProgress(0);
            log.setStartTime(null);
            log.setEndTime(null);
            log.setProcessingTimeSec(null);

            // Audit
            log.setAdddate(now);
            log.setAddwho(user);
            log.setEditdate(now);
            log.setEditwho(user);

            logRepo.save(log);

            // {logId}__filename.xlsx
            String storedName =
                    log.getId() + "__" + file.getOriginalFilename();

            file.transferTo(new File(INPUT_PATH, storedName));

            return ResponseEntity.ok(
                Map.of(
                    "refId", log.getId(),
                    "status", "QUEUED",
                    "message", "File uploaded and queued"
                )
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /* =====================================================
       STATUS API
       ===================================================== */
    @GetMapping("/status")
    public ResponseEntity<FileUploadLog> getStatus(
            @RequestParam Long refId
    ) {
        return logRepo.findById(refId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /* =====================================================
       FULL HISTORY
       ===================================================== */
    @GetMapping("/history/all")
    public ResponseEntity<List<FileUploadLog>> getAllHistory() {
        return ResponseEntity.ok(
                logRepo.findAllByOrderByLogDateDesc()
        );
    }

    /* =====================================================
       ANALYTICS
       ===================================================== */
    @GetMapping("/analytics")
    public ResponseEntity<UploadAnalyticsDTO> getAnalytics() {

        List<FileUploadLog> logs =
                logRepo.findAllByOrderByLogDateDesc();

        UploadAnalyticsDTO dto = new UploadAnalyticsDTO();

        dto.totalFiles = logs.size();
        dto.successCount = logs.stream()
                .filter(l -> "SUCCESS".equals(l.getStatus()))
                .count();

        dto.errorCount = logs.stream()
                .filter(l -> "ERROR".equals(l.getStatus()))
                .count();

        dto.processingTimeTrend = logs.stream()
                .filter(l -> l.getProcessingTimeSec() != null)
                .limit(10)
                .map(FileUploadLog::getProcessingTimeSec)
                .collect(Collectors.toList());

        dto.avgProcessingTime = logs.stream()
                .filter(l -> l.getProcessingTimeSec() != null)
                .mapToLong(FileUploadLog::getProcessingTimeSec)
                .average()
                .orElse(0);

        dto.lastProcessingTime = logs.stream()
                .filter(l -> l.getProcessingTimeSec() != null)
                .findFirst()
                .map(FileUploadLog::getProcessingTimeSec)
                .orElse(0L);

        return ResponseEntity.ok(dto);
    }

    /* =====================================================
       ERROR REPORT
       ===================================================== */
    @GetMapping(value = "/error-report", produces = "text/csv")
    public void downloadErrorReport(
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/csv");
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=excel-error-report.csv"
        );

        List<FileUploadLog> errors =
                logRepo.findByStatusOrderByLogDateDesc("ERROR");

        PrintWriter writer = response.getWriter();

        writer.println(
            "RefId,Filename,Validation,Message,StartTime,EndTime,ProcessingTime(sec)"
        );

        for (FileUploadLog log : errors) {
            writer.println(
                log.getId() + "," +
                safe(log.getFilename()) + "," +
                safe(log.getValidationPoint()) + "," +
                safe(log.getErrorMessage()) + "," +
                safe(log.getStartTime()) + "," +
                safe(log.getEndTime()) + "," +
                safe(log.getProcessingTimeSec())
            );
        }

        writer.flush();
        writer.close();
    }

    private String safe(Object o) {
        return o == null ? "" : o.toString().replace(",", " ");
    }
}
