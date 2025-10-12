package com.example.SafeTag_BE.exception;

public class QrNotFoundException extends  RuntimeException{
    public QrNotFoundException() {
        super("Qr not found");
    }
    public QrNotFoundException(String m){
        super(m);
    }

}
