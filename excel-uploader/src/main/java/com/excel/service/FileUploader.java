package com.excel.service;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.excel.entity.FileUploadLog;
import com.excel.repository.FileUploadLogRepository;

@Service
public class FileUploader {

    private static final Logger log =
            LoggerFactory.getLogger(FileUploader.class);

    // {logId}__original.xlsx
    private static final Pattern FILE_PATTERN =
            Pattern.compile("^(\\d+)__(.+\\.xlsx)$");

    @Autowired
    private FileProcessingService fileProcessingService;

    @Autowired
    private FileUploadLogRepository logRepo;

    @Value("${excel.read.path}")
    private String FILE_PATH;

    @Value("${excel.move.path}")
    private String MOVE_PATH;

    @Value("${excel.error.path}")
    private String ERROR_PATH;

    @Scheduled(
        fixedDelayString = "${excel.interval.time}",
        initialDelayString = "${excel.sleep.time}"
    )
    public void processFiles() {

        log.info("📂 Scheduler started");

        File[] files = new File(FILE_PATH).listFiles();
        if (files == null || files.length == 0) {
            log.info("📭 No files found");
            return;
        }

        for (File file : files) {

            if (!file.isFile()) continue;

            String fileName = file.getName();

            // hidden/system files
            if (file.isHidden()
                    || fileName.startsWith(".")
                    || "Thumbs.db".equalsIgnoreCase(fileName)
                    || "desktop.ini".equalsIgnoreCase(fileName)) {
                continue;
            }

            Matcher matcher = FILE_PATTERN.matcher(fileName);
            if (!matcher.matches()) {
                log.warn("❌ Invalid file format: {}", fileName);
                continue;
            }

            Long logId = Long.parseLong(matcher.group(1));
            String originalFilename = matcher.group(2);

            if (!isFileStable(file)) {
                log.info("⏳ File still copying: {}", fileName);
                continue;
            }

            FileUploadLog logEntry =
                    logRepo.findById(logId).orElse(null);

            if (logEntry == null) {
                log.warn("❌ No DB log for logId {}", logId);
                continue;
            }

            /* 🔥🔥🔥 FINAL FIX 🔥🔥🔥
               ONLY FINISHED FILES ARE SKIPPED
            */
            if ("SUCCESS".equals(logEntry.getStatus())
                    || "ERROR".equals(logEntry.getStatus())) {

                log.info(
                    "⏭ Skipping completed file logId {} status {}",
                    logId,
                    logEntry.getStatus()
                );
                continue;
            }

            int KSA = originalFilename.contains("KSA") ? 1 : 0;

            File successFile =
                    new File(MOVE_PATH, fileName);

            File errorFile =
                    new File(ERROR_PATH, fileName);

            try {
                log.info("🚀 Processing logId {}", logId);

                fileProcessingService.readFileTransactional(
                        logId,
                        file,
                        successFile,
                        errorFile,
                        System.currentTimeMillis(),
                        KSA
                );

            } catch (Exception e) {
                log.error("🔥 Scheduler error for logId {}", logId, e);
                try {
                    moveFallback(file, errorFile);
                } catch (Exception ex) {
                    log.error("❌ Fallback move failed", ex);
                }
            }

            // one file per cycle
            break;
        }

        log.info("✅ Scheduler completed");
    }

    /* ================= UTIL ================= */

    private boolean isFileStable(File file) {
        try {
            long s1 = file.length();
            Thread.sleep(3000);
            long s2 = file.length();
            return s1 == s2;
        } catch (Exception e) {
            return false;
        }
    }

    private void moveFallback(File src, File dest) throws Exception {
        dest.getParentFile().mkdirs();
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            in.transferTo(out);
        }
        src.delete();
    }
}
