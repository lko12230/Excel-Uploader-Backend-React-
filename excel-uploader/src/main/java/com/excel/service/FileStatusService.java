package com.excel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.excel.repository.FileUploadLogRepository;

@Service
public class FileStatusService {

    @Autowired
    private FileUploadLogRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessing(Long logId) {
        return repo.markProcessingOnce(logId) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFinalizing(Long logId) {
        repo.markFinalizing(logId, 97);
    }
}
