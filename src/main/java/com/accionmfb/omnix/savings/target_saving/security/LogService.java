package com.accionmfb.omnix.savings.target_saving.security;

import org.springframework.stereotype.Service;

/**
 *
 * @author dofoleta
 */
@Service
public interface LogService {

    public void logInfo(String app, String token, String logMessage, String logType, String requestId);

    public void logError(String app, String token, String logMessage, String logType, String requestId);
}
