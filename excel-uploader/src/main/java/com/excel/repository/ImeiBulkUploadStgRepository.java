package com.excel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.excel.entity.ImeiBulkUploadStg;


@Repository
public interface ImeiBulkUploadStgRepository
        extends JpaRepository<ImeiBulkUploadStg, Long> {
	
    @Query("select coalesce(max(e.version),0) " +
            "from ImeiBulkUploadStg e where e.filename = :filename")
     Integer findMaxVersionByFilename(@Param("filename") String filename);

    // 🔹 future use (optional)
    // List<ImeiBulkUploadStg> findByFilename(String filename);

    // Integer findMaxVersionByFilename(String filename);
}
