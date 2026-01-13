package com.haochen.kvstore.exception;

public class CapacityExceededException extends KVStoreException{
    public CapacityExceededException(int capacity){
        super("KVStore capacity exceeded. Capacity= " + capacity);
    }
}