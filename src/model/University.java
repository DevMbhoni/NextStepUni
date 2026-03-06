package model;

import java.time.LocalDate;

public class University {
    private int universityID;
    private String uniName;
    private String location;
    private LocalDate applicationDeadline;
    private String description;
    private String websiteLink;
    private String uniPicturePath;

    public University(int universityID, String uniName, String location, LocalDate applicationDeadline, String websiteLink) {
        this.universityID = universityID;
        this.uniName = uniName;
        this.location = location;
        this.applicationDeadline = applicationDeadline;
        this.websiteLink = websiteLink;
    }

    public University(int universityID, String uniName, String location, LocalDate applicationDeadline, String description, String websiteLink, String uniPicturePath) {
        this.universityID = universityID;
        this.uniName = uniName;
        this.location = location;
        this.applicationDeadline = applicationDeadline;
        this.description = description;
        this.websiteLink = websiteLink;
        this.uniPicturePath = uniPicturePath;
    }

    public University(int universityID, String uniName) {
        this.universityID = universityID;
        this.uniName = uniName;
    }

    public void setUniversityID(int universityID) {
        this.universityID = universityID;
    }

    public void setUniName(String universityName) {
        this.uniName = universityName;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setUniPicturePath(String uniPicturePath) {
        this.uniPicturePath = uniPicturePath;
    }
    public String getUniPicturePath() {
        return uniPicturePath;
    }
    public int getUniversityID() {
        return universityID;
    }

    public String getUniName() {
        return uniName;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public String getWebsiteLink() {
        return websiteLink;
    }
}