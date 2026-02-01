package com.atypon.monitoring;

public interface AlertService {

    void alert(String key, String message, Throwable error);
}
