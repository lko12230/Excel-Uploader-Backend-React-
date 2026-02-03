package com.excel.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.excel.entity.FileUploadLog;
import com.excel.repository.FileUploadLogRepository;

@Service
public class FileFinalizeService {

    @Autowired
    private FileUploadLogRepository logRepo;

    /**
     * 🔥 ABSOLUTE FINAL COMMIT
     * - Completely isolated transaction
     * - No lock interference
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeSuccess(
            Long logId,
            String validation,
            String message
    ) {

        FileUploadLog log = logRepo.findById(logId).orElseThrow();

        if ("SUCCESS".equals(log.getStatus())
                || "ERROR".equals(log.getStatus())) {
            return;
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start =
                log.getStartTime() != null
                        ? log.getStartTime()
                        : end.minusSeconds(1);

        log.setStatus("SUCCESS");
        log.setProgress(100);
        log.setValidationPoint(validation);
        log.setErrorMessage(message);
        log.setEndTime(end);
        log.setEditdate(end);
        log.setEditwho("SYSTEM");

        long sec = Duration.between(start, end).toSeconds();
        log.setProcessingTimeSec(Math.max(1, sec));

        logRepo.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeError(
            Long logId,
            String validation,
            String message
    ) {

        FileUploadLog log = logRepo.findById(logId).orElseThrow();

        if ("SUCCESS".equals(log.getStatus())
                || "ERROR".equals(log.getStatus())) {
            return;
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start =
                log.getStartTime() != null
                        ? log.getStartTime()
                        : end.minusSeconds(1);

        log.setStatus("ERROR");
        log.setProgress(100);
        log.setValidationPoint(validation);
        log.setErrorMessage(message);
        log.setEndTime(end);
        log.setEditdate(end);
        log.setEditwho("SYSTEM");

        long sec = Duration.between(start, end).toSeconds();
        log.setProcessingTimeSec(Math.max(1, sec));

        logRepo.save(log);
    }
}
