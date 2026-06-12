package com.example.troubleticket.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Note {
    private String id;
    private String date;
    private String author;
    private String text;
    private String type;

    public Note() {
        this.id = UUID.randomUUID().toString();
        this.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
