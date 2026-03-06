package model;

import java.time.LocalDate;

public class BursaryReview implements Review {
    private int bursaryReviewID;
    private int bursaryID;
    private int studentID;
    private String content;
    private int rating;
    private LocalDate datePosted;

    public BursaryReview(int bursaryReviewID, int bursaryID, int studentID, String content, int rating, LocalDate datePosted) {
        this.bursaryReviewID = bursaryReviewID;
        this.bursaryID = bursaryID;
        this.studentID = studentID;
        this.content = content;
        this.rating = rating;
        this.datePosted = datePosted;
    }

    public BursaryReview(int reviewId, int bursaryId, String content, LocalDate datePosted, int rating) {
        this.bursaryReviewID = reviewId;
        this.bursaryID = bursaryId;
        this.content = content;
        this.datePosted = datePosted;
        this.rating = rating;
    }

    public int getBursaryReviewID() {
        return bursaryReviewID;
    }

    public void setBursaryReviewID(int bursaryReviewID) {
        this.bursaryReviewID = bursaryReviewID;
    }

    public int getBursaryID() {
        return bursaryID;
    }

    public void setBursaryID(int bursaryID) {
        this.bursaryID = bursaryID;
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


    public LocalDate getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(LocalDate datePosted) {
        this.datePosted = datePosted;
    }

    @Override public String getContent() { return content; }
    @Override public int getRating() { return rating; }
    @Override public void setRating(int rating) { this.rating = rating; }
    @Override public int getId() { return bursaryReviewID; }
}