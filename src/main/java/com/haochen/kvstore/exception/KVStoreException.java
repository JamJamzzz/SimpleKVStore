package com.haochen.kvstore.exception;

public abstract class KVStoreException extends RuntimeException{
    public KVStoreException(String message) {
        super(message);
    }

    public KVStoreException(String message, Throwable cause){
        super(message, cause);
    }
}
