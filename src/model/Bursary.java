package model;


import java.time.LocalDate;

public class Bursary {


    private int bursaryID;
    private String burName;
    private LocalDate applicationDeadline;
    private String description;
    private String websiteLink;

    public Bursary(int bursaryID, String burName, LocalDate applicationDeadline, String description, String websiteLink) {
        this.bursaryID = bursaryID;
        this.burName = burName;
        this.applicationDeadline = applicationDeadline;
        this.description = description;
        this.websiteLink = websiteLink;
    }

    public void setBurName(String burName) {
        this.burName = burName;
    }
    public int getBursaryID() {
        return bursaryID;
    }
    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWebsiteLink(String websiteLink) {
        this.websiteLink = websiteLink;
    }

    public Bursary(String burName, LocalDate applicationDeadline, String description, String websiteLink) {
        this.burName = burName;
        this.applicationDeadline = applicationDeadline;
        this.description = description;
        this.websiteLink = websiteLink;
    }

    public String getBurName() {
        return burName;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public String getDescription() {
        return description;
    }

    public String getWebsiteLink() {
        return websiteLink;
    }
}
