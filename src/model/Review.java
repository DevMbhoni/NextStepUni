package model;

import java.time.LocalDate;

public interface Review {
    String getContent();
    int getRating();
    void setRating(int rating);
    int getId();
    LocalDate getDatePosted();
    void setContent(String updatedText);
}