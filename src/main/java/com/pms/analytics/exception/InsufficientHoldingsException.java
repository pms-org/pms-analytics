package com.pms.analytics.exception;

public class InsufficientHoldingsException extends RuntimeException {
     
    public InsufficientHoldingsException(String message) {
        super(message); 
    }
   
    public InsufficientHoldingsException(String message, Throwable cause) {
        super(message, cause);
    }
}
