package com.haochen.kvstore.exception;

public class KeyNotFoundException extends KVStoreException{
    public KeyNotFoundException(Object key){
        super("key not found" + key);
    }
}
