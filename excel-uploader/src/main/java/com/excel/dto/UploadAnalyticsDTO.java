package com.excel.dto;

import java.util.List;

public class UploadAnalyticsDTO {

    public long totalFiles;
    public long successCount;
    public long errorCount;

    public double avgProcessingTime;
    public double lastProcessingTime;

    public List<Long> processingTimeTrend; // seconds
}
