package model;

import java.time.LocalDate;

public class UniversityReview implements Review {
    private int uniReviewID;
    private int universityID;
    private int studentID;
    private String content;
    private int rating;
    private LocalDate datePosted;

    public UniversityReview(int universityID, int uniReviewID, int studentID, String content, int rating, LocalDate datePosted) {
        this.universityID = universityID;
        this.uniReviewID = uniReviewID;
        this.studentID = studentID;
        this.content = content;
        this.rating = rating;
        this.datePosted = datePosted;
    }

    public UniversityReview(int uniReviewID, String content, int rating, LocalDate datePosted) {
        this.uniReviewID = uniReviewID;
        this.content = content;
        this.rating = rating;
        this.datePosted = datePosted;
    }
    public int getUniReviewID() {
        return uniReviewID;
    }

    public void setUniReviewID(int uniReviewID) {
        this.uniReviewID = uniReviewID;
    }

    public int getUniversityID() {
        return universityID;
    }

    public void setUniversityID(int universityID) {
        this.universityID = universityID;
    }

    public int getStudentID() {
        return studentID;
    }

    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override public String getContent() { return content; }
    @Override public int getRating() { return rating; }
    @Override public void setRating(int rating) { this.rating = rating; }
    @Override public int getId() { return uniReviewID; }

    public LocalDate getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(LocalDate datePosted) {
        this.datePosted = datePosted;
    }

    // Constructors, Getters, Setters
}