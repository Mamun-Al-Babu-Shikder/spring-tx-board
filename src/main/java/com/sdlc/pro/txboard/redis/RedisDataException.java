package com.sdlc.pro.txboard.redis;

public class RedisDataException extends RuntimeException {
    public RedisDataException(String message) {
        super(message);
    }

    public RedisDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
