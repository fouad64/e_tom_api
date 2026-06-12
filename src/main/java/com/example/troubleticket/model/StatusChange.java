package com.example.troubleticket.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StatusChange {
    private Long id;
    private String status;
    private String changeReason;
    private String changeDate;
    private String type;

    private static long idCounter = 1;

    public StatusChange() {
        this.id = idCounter++;
        this.changeDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
    }

    public StatusChange(String status, String changeReason, String type) {
        this();
        this.status = status;
        this.changeReason = changeReason;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(String changeDate) {
        this.changeDate = changeDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
