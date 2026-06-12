package com.example.troubleticket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TroubleTicket {
    private String id;
    private String href;
    private String name;
    private String externalId;
    private String ticketType;
    private String description;
    private String severity;
    private String priority;
    private String status;
    private String statusChangeReason;
    private String creationDate;
    private String lastUpdate;
    private String requestedResolutionDate;
    private String expectedResolutionDate;
    private String resolutionDate;

    @JsonProperty("@baseType")
    private String baseType;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("@schemaLocation")
    private String schemaLocation;

    private Channel channel;
    private List<Note> note = new ArrayList<>();
    private List<StatusChange> statusChange = new ArrayList<>();
    private List<Attachment> attachment = new ArrayList<>();
    private List<RelatedParty> relatedParty = new ArrayList<>();
    private List<RelatedEntity> relatedEntity = new ArrayList<>();
    private List<TicketRelationship> ticketRelationship = new ArrayList<>();

    public TroubleTicket() {
        this.id = UUID.randomUUID().toString();
        this.status = "Acknowledged";
        this.baseType = "TroubleTicket";
        this.type = "TroubleTicket";
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
        this.creationDate = now;
        this.lastUpdate = now;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusChangeReason() {
        return statusChangeReason;
    }

    public void setStatusChangeReason(String statusChangeReason) {
        this.statusChangeReason = statusChangeReason;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getRequestedResolutionDate() {
        return requestedResolutionDate;
    }

    public void setRequestedResolutionDate(String requestedResolutionDate) {
        this.requestedResolutionDate = requestedResolutionDate;
    }

    public String getExpectedResolutionDate() {
        return expectedResolutionDate;
    }

    public void setExpectedResolutionDate(String expectedResolutionDate) {
        this.expectedResolutionDate = expectedResolutionDate;
    }

    public String getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(String resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public List<Note> getNote() {
        return note;
    }

    public void setNote(List<Note> note) {
        this.note = note;
    }

    public List<StatusChange> getStatusChange() {
        return statusChange;
    }

    public void setStatusChange(List<StatusChange> statusChange) {
        this.statusChange = statusChange;
    }

    public List<Attachment> getAttachment() {
        return attachment;
    }

    public void setAttachment(List<Attachment> attachment) {
        this.attachment = attachment;
    }

    public List<RelatedParty> getRelatedParty() {
        return relatedParty;
    }

    public void setRelatedParty(List<RelatedParty> relatedParty) {
        this.relatedParty = relatedParty;
    }

    public List<RelatedEntity> getRelatedEntity() {
        return relatedEntity;
    }

    public void setRelatedEntity(List<RelatedEntity> relatedEntity) {
        this.relatedEntity = relatedEntity;
    }

    public List<TicketRelationship> getTicketRelationship() {
        return ticketRelationship;
    }

    public void setTicketRelationship(List<TicketRelationship> ticketRelationship) {
        this.ticketRelationship = ticketRelationship;
    }
}
