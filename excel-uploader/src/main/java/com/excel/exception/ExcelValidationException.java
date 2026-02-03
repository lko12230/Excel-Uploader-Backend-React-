package com.excel.exception;

@SuppressWarnings("serial")
public class ExcelValidationException extends RuntimeException {

    private final String validationCode;

    public ExcelValidationException(String validationCode, String message) {
        super(message);
        this.validationCode = validationCode;
    }

    public String getValidationCode() {
        return validationCode;
    }
}
