package com.excel.service;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.excel.config.Mail3;
import com.excel.entity.FileUploadLog;
import com.excel.entity.ImeiBulkUploadStg;
import com.excel.exception.ExcelValidationException;
import com.excel.repository.FileUploadLogRepository;
import com.excel.repository.ImeiBulkUploadStgRepository;

@Service
public class FileProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(FileProcessingService.class);

    @Autowired
    private ImeiBulkUploadStgRepository imeiRepo;

    @Autowired
    private FileUploadLogRepository logRepo;

    @Autowired
    private FileProgressService fileProgressService;

    @Autowired
    private FileStatusService fileStatusService;

    @Autowired
    private Mail3 mail3;
    
    @Autowired
    private FileFinalizeService fileFinalizeService;

    @Value("${app.mail.to}")
    private String EMAIL_TO;

    @Value("${app.mail.to1}")
    private String EMAIL_TO1;

    @Value("${excel.file.timeout.ms}")
    private long FILE_TIMEOUT_MS;

    /* =====================================================
       MAIN FILE PROCESS
       ===================================================== */
    @Transactional
    public void readFileTransactional(
            Long logId,
            File sourceFile,
            File successFile,
            File errorFile,
            long requestStartMs,
            int KSA
    ) {

        FileUploadLog initialLog =
                logRepo.findById(logId)
                       .orElseThrow(() ->
                           new IllegalStateException("No log found: " + logId));

        if (!"QUEUED".equals(initialLog.getStatus())) {
            return;
        }

        /* 🔥 ATOMIC PICK (committed immediately) */
        boolean picked = fileStatusService.markProcessing(logId);
        if (!picked) return;

        fileProgressService.updateProgress(logId, 5);

        String filename = initialLog.getFilename();

        try (FileInputStream fis = new FileInputStream(sourceFile);
             Workbook workbook = WorkbookFactory.create(fis)) {

            if (!filename.startsWith("SERIALUPLOADINB")
                    && !filename.startsWith("SERIALUPLOADOUT")) {
                throw new ExcelValidationException(
                        "INVALID_FILE_NAME",
                        "Invalid file name: " + filename
                );
            }

            fileProgressService.updateProgress(logId, 10);

            int version =
                    Optional.ofNullable(
                            imeiRepo.findMaxVersionByFilename(filename))
                            .orElse(0) + 1;

            List<ImeiBulkUploadStg> batchList = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {

                Sheet sheet = workbook.getSheetAt(s);

                if (sheet.getLastRowNum() > 200000) {
                    throw new ExcelValidationException(
                            "ROW_LIMIT_EXCEEDED",
                            "Excel contains more than 200,000 rows"
                    );
                }

                int totalRows = sheet.getLastRowNum();
                int processed = 0;
                int lastPercent = 10;
                long lastUpdate = System.currentTimeMillis();

                for (Row row : sheet) {

                    if (System.currentTimeMillis() - requestStartMs
                            > FILE_TIMEOUT_MS) {
                        throw new ExcelValidationException(
                                "FILE_PROCESSING_TIMEOUT",
                                "Processing timeout exceeded"
                        );
                    }

                    if (row.getRowNum() == 0) {
                        validateHeader(filename, row, formatter);
                        continue;
                    }

                    ImeiBulkUploadStg e = new ImeiBulkUploadStg();
                    e.setStorerKey(formatter.formatCellValue(row.getCell(0)));
                    e.setWhse(formatter.formatCellValue(row.getCell(1)));

                    if (filename.startsWith("SERIALUPLOADINB")) {
                        e.setAsn(formatter.formatCellValue(row.getCell(2)));
                        e.setAsnLine(formatter.formatCellValue(row.getCell(3)));
                        e.setSku(formatter.formatCellValue(row.getCell(4)));
                        e.setItemId(formatter.formatCellValue(row.getCell(5)));
                        e.setSerial(formatter.formatCellValue(row.getCell(6)));
                        e.setParentSerial(formatter.formatCellValue(row.getCell(7)));
                        e.setType("I");
                    } else {
                        e.setOrderKey(formatter.formatCellValue(row.getCell(2)));
                        e.setSku(formatter.formatCellValue(row.getCell(3)));
                        e.setItemId(formatter.formatCellValue(row.getCell(4)));
                        e.setSerial(formatter.formatCellValue(row.getCell(5)));
                        e.setParentSerial(formatter.formatCellValue(row.getCell(6)));
                        e.setType("O");
                    }

                    e.setFilename(filename);
                    e.setVersion(version);
                    e.setAddDate(new Date());
                    e.setAddWho("SYSTEM");
                    e.setEditDate(new Date());
                    e.setEditWho("SYSTEM");
                    e.setProcessFlag(0);

                    batchList.add(e);
                    processed++;

                    long now = System.currentTimeMillis();
                    if (processed % 200 == 0 || now - lastUpdate > 500) {
                        int percent = (int) ((processed * 100.0) / totalRows);
                        if (percent > lastPercent && percent < 95) {
                            fileProgressService.updateProgress(logId, percent);
                            lastPercent = percent;
                            lastUpdate = now;
                        }
                    }
                }
            }

            if (batchList.isEmpty()) {
                throw new ExcelValidationException(
                        "NO_DATA_FOUND",
                        "Excel contains no data rows"
                );
            }

            imeiRepo.saveAll(batchList);
            imeiRepo.flush();

         // 👇 UI ko 95 se bahar nikaalne ke liye
            logRepo.markFinalizing(logId, 97);
            /* ================= FINAL VALUES FOR LAMBDA ================= */
            final Long logIdFinal = logId;
            final String filenameFinal = filename;
            final int ksaFinal = KSA;

            TransactionSynchronizationManager.registerSynchronization(
            	    new TransactionSynchronization() {
            	        @Override
            	        public void afterCommit() {
            	            try {
            	                fileFinalizeService.finalizeSuccess(
            	                        logIdFinal,
            	                        "SUCCESS",
            	                        "IMEI records inserted successfully"
            	                );

            	                moveFile(sourceFile, successFile);

            	                sendMail(
            	                        "SUCCESS",
            	                        "SUCCESS",
            	                        filenameFinal,
            	                        "IMEI records inserted successfully",
            	                        logIdFinal,
            	                        ksaFinal
            	                );
            	            } catch (Exception e) {
            	                log.error("Post commit success failed", e);
            	            }
            	        }
            	    });


        } catch (Exception ex) {

            for (int p : new int[]{20, 40, 60, 80, 95}) {
                fileProgressService.updateProgress(logId, p);
            }

            String validationTmp = "PROCESSING_FAILED";
            String messageTmp = ex.getMessage();

            if (ex instanceof ExcelValidationException eve) {
                validationTmp = eve.getValidationCode();
                messageTmp = eve.getMessage();
            }

            final String validationFinal = validationTmp;
            final String messageFinal = messageTmp;
            final Long logIdFinal = logId;

            TransactionSynchronizationManager.registerSynchronization(
            	    new TransactionSynchronization() {
            	        @Override
            	        public void afterCommit() {
            	            try {
            	                fileFinalizeService.finalizeError(
            	                        logIdFinal,
            	                        validationFinal,
            	                        messageFinal
            	                );

            	                moveFile(sourceFile, errorFile);

            	                sendMail(
            	                        "ERROR",
            	                        validationFinal,
            	                        filename,
            	                        messageFinal,
            	                        logIdFinal,
            	                        KSA
            	                );
            	            } catch (Exception e) {
            	                log.error("Post commit error failed", e);
            	            }
            	        }
            	    });
        }
    }

    /* =====================================================
       FINALIZE LOG
       ===================================================== */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void finalizeLog(
            Long logId,
            String status,
            String validation,
            String message) {

        FileUploadLog logEntry =
                logRepo.findById(logId).orElseThrow();

        LocalDateTime end = LocalDateTime.now();

        // ✅ FINAL STATUS
        logEntry.setStatus(status);          // SUCCESS / ERROR
        logEntry.setProgress(100);           // 👈 YAHI PE

        logEntry.setValidationPoint(validation);
        logEntry.setErrorMessage(message);
        logEntry.setEndTime(end);
        logEntry.setEditdate(end);
        logEntry.setEditwho("SYSTEM");

        long millis =
                Duration.between(logEntry.getStartTime(), end).toMillis();

        logEntry.setProcessingTimeSec(Math.max(1, millis / 1000));

        logRepo.save(logEntry);
    }


    /* ================= UTIL ================= */

    private void validateHeader(String filename, Row row, DataFormatter f) {

        String[] expected = filename.startsWith("SERIALUPLOADINB")
                ? new String[]{"STORERKEY","WHSE","ASN","LINE","SKU","ID","SERIAL","PARENTSERIAL"}
                : new String[]{"STORERKEY","WHSE","ORDERKEY","SKU","ID","SERIAL","PARENTSERIAL"};

        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equalsIgnoreCase(
                    f.formatCellValue(row.getCell(i)))) {
                throw new ExcelValidationException(
                        "HEADER_VALIDATION",
                        "Invalid Excel header at column " + (i + 1)
                );
            }
        }
    }

    private static void moveFile(File src, File dest) throws Exception {
        dest.getParentFile().mkdirs();
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        src.delete();
    }


    private void sendMail(
            String severity,
            String validation,
            String filename,
            String details,
            long refId,
            int KSA) {

        String body = buildEnterpriseMail(
                severity, validation, filename, details, refId);

        mail3.sendMailSSL(
                KSA == 1 ? EMAIL_TO : EMAIL_TO1,
                "[" + severity + "] WMS Serial Upload – " + filename,
                body
        );
    }
    // buildEnterpriseMail() — SAME as your existing implementation
    // 🔽 buildEnterpriseMail() — SAME as your existing code (unchanged)

    /* =========================================================
    🧾 ENTERPRISE MAIL TEMPLATE
    ========================================================= */
 private String buildEnterpriseMail(
         String severity,        // SUCCESS / ERROR / INFO
         String validation,
         String filename,
         String details,
         long referenceId
 ) {

     // -------- Defaults --------
     String gradient;
     String badge;
     String introText;
     boolean showReferenceId = true;

     // -------- Severity Handling --------
     if ("SUCCESS".equalsIgnoreCase(severity)) {
         gradient = "linear-gradient(135deg,#22c55e,#16a34a)";
         badge = "SUCCESS";
         introText = "The serial Excel file was processed successfully.";
       //  showReferenceId = false;
     }
     else if ("ERROR".equalsIgnoreCase(severity)) {
         gradient = "linear-gradient(135deg,#ef4444,#dc2626)";
         badge = "ERROR";
         introText = "An error was detected during serial Excel processing.";
     }
     else { // INFO / WARNING
         gradient = "linear-gradient(135deg,#fbbf24,#f59e0b)";
         badge = "WARNING";
         introText = "Please review the following information.";
     }

     String formattedDate =
             java.time.LocalDateTime.now()
                     .format(java.time.format.DateTimeFormatter.ofPattern(
                             "dd MMM yyyy, hh:mm:ss a"
                     ));

     String referenceBlock = showReferenceId
             ? "<div style='padding:12px 14px;background:#f3f4f6;border-radius:8px;"
             + "font-size:12px;color:#111827;'>"
             + "<b>Reference ID:</b>"
             + "<span style='margin-left:6px;font-family:Menlo,Consolas,monospace;"
             + "background:#111827;color:#ffffff;padding:4px 8px;border-radius:6px;'>"
             + referenceId
             + "</span></div>"
             : "";

     // -------- HTML --------
     return ""
         + "<!DOCTYPE html>"
         + "<html>"
         + "<body style='margin:0;padding:0;background:#e5e7eb;"
         + "font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Arial;'>"

         + "<table width='100%' cellspacing='0' cellpadding='0'>"
         + "<tr><td align='center' style='padding:40px 12px;'>"

         // ===== CARD =====
         + "<table width='700' cellspacing='0' cellpadding='0' "
         + "style='background:#ffffff;border-radius:12px;"
         + "box-shadow:0 18px 45px rgba(0,0,0,0.12);overflow:hidden;'>"

         // ===== HEADER =====
         + "<tr><td style='padding:26px 30px;background:" + gradient + ";'>"
         + "<table width='100%'><tr>"

         + "<td style='color:#ffffff;'>"
         + "<div style='font-size:20px;font-weight:700;'>Warehouse Management System</div>"
         + "<div style='font-size:12px;opacity:0.9;margin-top:6px;'>"
         + "Automated Serial Integration</div>"
         + "</td>"

         + "<td align='right'>"
         + "<img src='https://images.assettype.com/fortune-india/import/company/logos/Uno%20Minda.png' "
         + "width='90' style='display:block;'/>"
         + "</td>"

         + "</tr></table></td></tr>"

         // ===== BODY =====
         + "<tr><td style='padding:32px 30px;background:#f9fafb;'>"

         + "<p style='font-size:14px;color:#111827;'>Hello Team,</p>"
         + "<p style='font-size:14px;color:#374151;'>" + introText + "</p>"

         // BADGE
         + "<span style='display:inline-block;padding:7px 16px;"
         + "background:" + gradient + ";color:#ffffff;font-size:11px;"
         + "font-weight:800;border-radius:999px;'>" + badge + "</span>"

         // DETAILS CARD
         + "<table width='100%' style='margin-top:26px;background:#ffffff;"
         + "border-radius:10px;box-shadow:0 6px 18px rgba(0,0,0,0.08);'>"
         + "<tr><td width='6' style='background:" + gradient + ";'></td>"
         + "<td style='padding:20px 22px;'>"

         + "<div style='font-size:12px;color:#6b7280;'>Validation</div>"
         + "<div style='font-size:14px;font-weight:700;color:#111827;margin-bottom:12px;'>"
         + validation + "</div>"

         + "<div style='font-size:12px;color:#6b7280;'>File Name</div>"
         + "<div style='font-size:13px;font-family:Menlo,Consolas,monospace;"
         + "color:#111827;margin-bottom:12px;'>" + filename + "</div>"

         + "<div style='font-size:12px;color:#6b7280;'>Details</div>"
         + "<div style='font-size:14px;color:#111827;margin-bottom:16px;'>"
         + details + "</div>"

         + referenceBlock

         + "</td></tr></table>"

         + "<p style='margin-top:26px;font-size:14px;color:#111827;'>"
         + "Regards,<br><b>WMS Integration Platform</b></p>"

         + "</td></tr>"

         // ===== FOOTER =====
         + "<tr><td style='padding:14px 24px;background:#111827;"
         + "font-size:11px;color:#d1d5db;text-align:center;'>"
         + "System Generated | " + formattedDate
         + "</td></tr>"

         + "</table></td></tr></table>"
         + "</body></html>";
 }
	
}
