package com.excel.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.excel.entity.FileUploadLog;
import com.excel.repository.FileUploadLogRepository;

@Service
public class FileProgressService {

    @Autowired
    private FileUploadLogRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgress(Long logId, int target) {

        FileUploadLog log = repo.findById(logId).orElse(null);
        if (log == null) return;

        if (!"PROCESSING".equals(log.getStatus())
                && !"FINALIZING".equals(log.getStatus())) {
            return;
        }

        int current = log.getProgress() == null ? 0 : log.getProgress();
        int safe = Math.min(Math.max(target, current + 1), 99);

        log.setProgress(safe);
        log.setEditdate(LocalDateTime.now());
        log.setEditwho("SYSTEM");

        repo.save(log);
    }
}
