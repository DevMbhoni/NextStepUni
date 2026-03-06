package util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.UniversityReview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class ReviewHelper {

    public static void loadAllReviews(VBox reviewsVBox, TextArea reviewTextArea,
                                      Button reviewBackButton, ScrollPane scrollPane, HBox labelsHbox) {
        List<UniversityReview> reviews = fetchReviews("SELECT UniReviewID, Content, Rating, DatePosted FROM UniversityReview");
        displayReviews(reviews, reviewsVBox, reviewTextArea, reviewBackButton, scrollPane, labelsHbox);
    }
    public static void loadReviewsForUniversity(int universityId, VBox reviewsVBox,
                                                TextArea reviewTextArea, Button reviewBackButton,
                                                ScrollPane scrollPane, HBox labelsHbox) {
        String sql = "SELECT UniReviewID, Content, Rating, DatePosted FROM UniversityReview WHERE UniversityID = ?";
        List<UniversityReview> reviews = fetchReviews(sql, universityId);
        displayReviews(reviews, reviewsVBox, reviewTextArea, reviewBackButton, scrollPane, labelsHbox);
    }


    private static List<UniversityReview> fetchReviews(String sql, Object... params) {
        List<UniversityReview> reviews = new ArrayList<>();

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UniversityReview review = new UniversityReview(
                        rs.getInt("UniReviewID"),
                        rs.getString("Content"),
                        rs.getInt("Rating"),
                        rs.getDate("DatePosted").toLocalDate()
                );
                reviews.add(review);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return reviews;
    }

    private static void displayReviews(List<UniversityReview> reviews, VBox reviewsVBox, TextArea reviewTextArea,
                                       Button reviewBackButton, ScrollPane scrollPane, HBox labelsHbox) {
        reviewsVBox.getChildren().clear();

        for (UniversityReview r : reviews) {
            HBox reviewItem = createReviewItem(r, reviewTextArea, reviewBackButton, scrollPane, labelsHbox);
            reviewsVBox.getChildren().add(reviewItem);
        }
    }


    private static HBox createReviewItem(UniversityReview review, TextArea reviewTextArea,
                                         Button reviewBackButton, ScrollPane scrollPane, HBox labelsHbox) {

        Label reviewLabel = new Label(review.getContent());
        reviewLabel.setFont(new Font(13));
        reviewLabel.setWrapText(true);
        reviewLabel.setMaxWidth(400);

        Label dateLabel = new Label("Posted on: " + review.getDatePosted());
        dateLabel.setFont(new Font(10));
        dateLabel.setStyle("-fx-font-style: italic;");
        dateLabel.setTranslateX(-110);

        Label ratingLabel = new Label(String.valueOf(review.getRating()));
        ratingLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        ratingLabel.setTranslateX(-40);

        HBox leftHBox = new HBox(reviewLabel);
        leftHBox.setAlignment(Pos.CENTER_LEFT);
        leftHBox.setPrefHeight(100);

        HBox rightHBox = new HBox(dateLabel, ratingLabel);
        rightHBox.setAlignment(Pos.CENTER_RIGHT);
        rightHBox.setSpacing(80);
        HBox.setHgrow(rightHBox, Priority.ALWAYS);

        HBox reviewContainer = new HBox(leftHBox, rightHBox);
        reviewContainer.setStyle("-fx-border-color: #d9d9d9; -fx-border-width: 0 0 1 0;");
        reviewContainer.setPrefWidth(600);
        reviewContainer.setPrefHeight(100);

        reviewLabel.setOnMouseClicked(event -> {
            reviewTextArea.setText(review.getContent());
            reviewTextArea.setVisible(true);
            reviewBackButton.setVisible(true);
            scrollPane.setVisible(false);
            labelsHbox.setVisible(false);
        });
        return reviewContainer;
    }
}
