package com.excel;

import java.io.File;

import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling 
@ComponentScan(basePackages = "com.excel")
public class ExcelApplication {

	 private static final Logger log =
	            LoggerFactory.getLogger(ExcelApplication.class);
	public static void main(String[] args) {
	    // ✅ CREATE LOG FOLDER BEFORE SPRING STARTS
        createLogDirectory();

		SpringApplication.run(ExcelApplication.class, args);
	}
	
	private static void createLogDirectory() {
        try {
            File logDir = new File("/Users/ayushgupta/Documents/excel/logs");
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                if (created) {
                    System.out.println(" Log directory created: " + logDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to create log directory");
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void onStartup() {
        log.info("=== WMS EXCEL FILE PROCESSOR STARTED | LOGGING INITIALIZED ===");
    }
    
    @PostConstruct
    public void initPoiLimit() {
        IOUtils.setByteArrayMaxOverride(200_000_000); // 200MB
        log.info("Apache POI byte array limit increased");
    }


}
