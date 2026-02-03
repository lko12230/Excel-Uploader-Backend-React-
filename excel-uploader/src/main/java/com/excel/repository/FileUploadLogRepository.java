package com.excel.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.excel.entity.FileUploadLog;

@Repository
public interface FileUploadLogRepository
        extends JpaRepository<FileUploadLog, Long> {

    /* =====================================================
       BASIC FETCH METHODS (UI / REPORTS)
       ===================================================== */

    // Latest status of a file (polling)
    FileUploadLog findTopByFilenameOrderByIdDesc(String filename);

    // History of a specific file
    List<FileUploadLog> findByFilenameOrderByLogDateDesc(String filename);

    // Full upload history (admin UI)
    List<FileUploadLog> findAllByOrderByLogDateDesc();

    // Error report for a file
    List<FileUploadLog> findByFilenameAndStatus(
            String filename,
            String status
    );

    // Global error report
    List<FileUploadLog> findByStatusOrderByLogDateDesc(String status);

    /* =====================================================
       🔥 ATOMIC PICK (QUEUED → PROCESSING)
       ===================================================== */

    /**
     * ✅ THREAD-SAFE + CRASH-SAFE
     * - sirf 1 thread file pick karega
     * - startTime sirf ek baar set hoga
     * - restart / duplicate safe
     *
     * @return 1 → picked successfully
     *         0 → already picked / processed
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE FileUploadLog f
           SET f.status = 'PROCESSING',
               f.startTime = CURRENT_TIMESTAMP,
               f.progress = 1,
               f.editdate = CURRENT_TIMESTAMP,
               f.editwho = 'SYSTEM'
         WHERE f.id = :logId
           AND f.status = 'QUEUED'
           AND f.startTime IS NULL
    """)
    int markProcessingOnce(@Param("logId") Long logId);

    /* =====================================================
       🔥 FINALIZING STATE (95% → 99%)
       ===================================================== */

    /**
     * Prevents UI from getting stuck at 95%
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE FileUploadLog f
           SET f.status = 'FINALIZING',
               f.progress = :progress,
               f.editdate = CURRENT_TIMESTAMP,
               f.editwho = 'SYSTEM'
         WHERE f.id = :logId
           AND f.status = 'PROCESSING'
    """)
    int markFinalizing(@Param("logId") Long logId,
                       @Param("progress") int progress);

    /* =====================================================
       🛠 RECOVERY SUPPORT (OPTIONAL BUT STRONG)
       ===================================================== */

    /**
     * Finds files stuck in PROCESSING
     * (used by recovery cron / admin job)
     */
    @Query("""
        SELECT f
          FROM FileUploadLog f
         WHERE f.status = 'PROCESSING'
           AND f.startTime < :cutoff
    """)
    List<FileUploadLog> findStuckProcessing(
            @Param("cutoff") LocalDateTime cutoff
    );
}
