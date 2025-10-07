package com.example.SafeTag_BE.exception;

public class QrGoneException extends RuntimeException{
    public QrGoneException(String message){
        super(message);
    }
    public QrGoneException(String message, Throwable cause){
        super(message, cause);
    }


}
