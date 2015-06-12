package com.odonataworkshop.files.renamer.file;

/**
 * User: maya
 * Date: 13-9-17
 */
public interface ProcessingHandler {
    void handleProcessStart(Object aObj);
    void handleProcessEnd(Object aObj);
    void handleBeforeStartProcess(Object aObj);
    void handleAfterEndProcess(Object aObj);
}
