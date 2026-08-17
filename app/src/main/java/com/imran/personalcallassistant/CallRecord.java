package com.imran.personalcallassistant;

public class CallRecord {
    private String phoneNumber;
    private String callerName;
    private String reason;
    private String language;
    private long timestamp;

    public CallRecord(String phoneNumber, String callerName, String reason, String language, long timestamp) {
        this.phoneNumber = phoneNumber;
        this.callerName = callerName;
        this.reason = reason;
        this.language = language;
        this.timestamp = timestamp;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getCallerName() { return callerName; }
    public String getReason() { return reason; }
    public String getLanguage() { return language; }
    public long getTimestamp() { return timestamp; }
}

