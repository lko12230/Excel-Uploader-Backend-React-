package com.excel.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "FILE_UPLOAD_LOG")
public class FileUploadLog {

    /* ================= PRIMARY KEY ================= */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /* ================= FILE INFO ================= */

    @Column(name = "FILENAME", nullable = false, length = 255)
    private String filename;

    /**
     * STATUS VALUES:
     * QUEUED | PROCESSING | SUCCESS | ERROR
     */
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    /**
     * HEADER_VALIDATION | ROW_LIMIT_EXCEEDED | SUCCESS | etc.
     */
    @Column(name = "VALIDATION_POINT", length = 100)
    private String validationPoint;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    /* ================= PROGRESS ================= */

    /**
     * 0–100
     */
    @Column(name = "PROGRESS")
    private Integer progress;

    /* ================= TIMING ================= */

    @Column(name = "LOG_DATE")
    private LocalDateTime logDate;

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Column(name = "PROCESSING_TIME_SEC")
    private Long processingTimeSec;

    /* ================= AUDIT FIELDS ================= */

    @Column(name = "ADD_DATE")
    private LocalDateTime adddate;

    @Column(name = "ADD_WHO", length = 100)
    private String addwho;

    @Column(name = "EDIT_DATE")
    private LocalDateTime editdate;

    @Column(name = "EDIT_WHO", length = 100)
    private String editwho;

    /* ================= CONSTRUCTORS ================= */

    public FileUploadLog() {
        this.logDate = LocalDateTime.now();
        this.adddate = LocalDateTime.now();
    }

    public FileUploadLog(
            String filename,
            String status,
            String validationPoint,
            String errorMessage,
            String addwho
    ) {
        this.filename = filename;
        this.status = status;
        this.validationPoint = validationPoint;
        this.errorMessage = errorMessage;
        this.progress = 0;

        this.logDate = LocalDateTime.now();
        this.startTime = LocalDateTime.now();

        this.adddate = LocalDateTime.now();
        this.addwho = addwho;
    }

    /* ================= GETTERS & SETTERS ================= */

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getValidationPoint() {
        return validationPoint;
    }

    public void setValidationPoint(String validationPoint) {
        this.validationPoint = validationPoint;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDateTime getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDateTime logDate) {
        this.logDate = logDate;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getProcessingTimeSec() {
        return processingTimeSec;
    }

    public void setProcessingTimeSec(Long processingTimeSec) {
        this.processingTimeSec = processingTimeSec;
    }

    public LocalDateTime getAdddate() {
        return adddate;
    }

    public void setAdddate(LocalDateTime adddate) {
        this.adddate = adddate;
    }

    public String getAddwho() {
        return addwho;
    }

    public void setAddwho(String addwho) {
        this.addwho = addwho;
    }

    public LocalDateTime getEditdate() {
        return editdate;
    }

    public void setEditdate(LocalDateTime editdate) {
        this.editdate = editdate;
    }

    public String getEditwho() {
        return editwho;
    }

    public void setEditwho(String editwho) {
        this.editwho = editwho;
    }
}
