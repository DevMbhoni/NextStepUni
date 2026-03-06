package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import model.BursaryReview;
import model.Review;
import model.UniversityReview;
import model.UserSession;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ResourceBundle;

public class MyReviewsController extends BaseController implements Initializable {


    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button ChooseMyUniReviews;
    @FXML private Button ChooseMyBursaryReviews;
    @FXML private TableView<Review> reviewTable;
    @FXML private TableColumn<Review, String> reviewContentCol;
    @FXML private TableColumn<Review, Integer> reviewRatingCol;
    @FXML private TableColumn<Review, Void> reviewActionCol;

    private final ObservableList<UniversityReview> uniReviewsList = FXCollections.observableArrayList();
    private final ObservableList<BursaryReview> bursaryReviewsList = FXCollections.observableArrayList();
    private FilteredList<UniversityReview> filteredUniReviews;
    private FilteredList<BursaryReview> filteredBursaryReviews;
    private String currentView = "University";
    private final int studentID = UserSession.getInstance().getId();

    private static final int MAX_RATING = 5;
    private static final String STAR_FILLED_PATH = "/images/icons/star_filled.png";
    private static final String STAR_EMPTY_PATH = "/images/icons/star_empty.png";
    private Image starFilledImage;
    private Image starEmptyImage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        loadStarImages();

        ChooseMyUniReviews.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: #ffffff;");
        sortComboBox.getItems().addAll("Date (Newest)", "Date (Oldest)", "Rating (Highest)", "Rating (Lowest)");
        sortComboBox.setValue("Date (Newest)");
        setupTableColumns();
        loadUniversityReviews();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
        sortComboBox.setOnAction(e -> applySorting());
    }

    private void loadStarImages() {
        try {
            starFilledImage = new Image(getClass().getResourceAsStream(STAR_FILLED_PATH));
            starEmptyImage = new Image(getClass().getResourceAsStream(STAR_EMPTY_PATH));
        } catch (Exception e) {
            System.err.println("Error loading star images. Make sure they exist in " + STAR_FILLED_PATH + " and " + STAR_EMPTY_PATH);
            starFilledImage = null;
            starEmptyImage = null;
        }
    }

    private void setupTableColumns() {
        reviewContentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContent()));

        reviewContentCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                    setWrapText(true);
                }
            }
        });

        reviewRatingCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getRating()).asObject());
        reviewRatingCol.setCellFactory(col -> createRatingCell());
        reviewActionCol.setCellFactory(col -> createActionCell());
    }

    private TableCell<Review, Integer> createRatingCell() {
        return new TableCell<>() {
            private final HBox starBox = new HBox(2);

            {
                starBox.setAlignment(Pos.CENTER);
                if (starFilledImage == null || starEmptyImage == null) {
                    setText("Rating N/A");
                }
            }

            @Override
            protected void updateItem(Integer rating, boolean empty) {
                super.updateItem(rating, empty);

                if (empty || rating == null || starFilledImage == null || starEmptyImage == null) {
                    setGraphic(null);
                } else {
                    Review currentReview = getTableView().getItems().get(getIndex());
                    updateStars(rating, currentReview);
                    setGraphic(starBox);
                }
            }

            private void updateStars(int currentRating, Review review) {
                starBox.getChildren().clear();
                for (int i = 1; i <= MAX_RATING; i++) {
                    ImageView star = new ImageView(i <= currentRating ? starFilledImage : starEmptyImage);
                    star.setFitHeight(20);
                    star.setFitWidth(20);
                    star.setPreserveRatio(true);
                    star.setCursor(Cursor.HAND);

                    final int ratingValue = i;
                    star.setOnMouseClicked(event -> {
                        updateRatingInDatabase(review, ratingValue);
                        updateStars(ratingValue, review);

                        review.setRating(ratingValue);
                    });
                    starBox.getChildren().add(star);
                }
            }
        };
    }

    private void applySearchFilter(String query) {
        String kw = query == null ? "" : query.toLowerCase().trim();
        if ("University".equals(currentView) && filteredUniReviews != null) {
            filteredUniReviews.setPredicate(r -> kw.isEmpty() || r.getContent().toLowerCase().contains(kw));
        } else if ("Bursary".equals(currentView) && filteredBursaryReviews != null) {
            filteredBursaryReviews.setPredicate(r -> kw.isEmpty() || r.getContent().toLowerCase().contains(kw));
        }
    }

    private void applySorting() {
        String sortOption = sortComboBox.getValue();
        if (sortOption == null) return;

        Comparator<Review> comparator = switch (sortOption) {
            case "Date (Newest)" -> Comparator.comparing(Review::getDatePosted).reversed();
            case "Date (Oldest)" -> Comparator.comparing(Review::getDatePosted);
            case "Rating (Highest)" -> Comparator.comparingInt(Review::getRating).reversed();
            case "Rating (Lowest)" -> Comparator.comparingInt(Review::getRating);
            default -> (r1, r2) -> 0;
        };

        if ("University".equals(currentView)) {
            FXCollections.sort(uniReviewsList, comparator);
        } else if ("Bursary".equals(currentView)) {
            FXCollections.sort(bursaryReviewsList, comparator);
        }
        reviewTable.refresh();
    }


    private void loadUniversityReviews() {
        currentView = "University";
        ChooseMyUniReviews.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: #ffffff;");
        ChooseMyBursaryReviews.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");

        uniReviewsList.clear();
        String sql = "SELECT r.UniReviewID, r.UniversityID, r.StudentID, r.Content, r.Rating, r.DatePosted " +
                "FROM UniversityReview r WHERE r.StudentID = ?";

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDate date = rs.getDate("DatePosted").toLocalDate();
                uniReviewsList.add(new UniversityReview(
                        rs.getInt("UniversityID"),
                        rs.getInt("UniReviewID"),
                        rs.getInt("StudentID"),
                        rs.getString("Content"),
                        rs.getInt("Rating"),
                        date
                ));
            }

            applySorting();

            if (filteredUniReviews == null) {
                filteredUniReviews = new FilteredList<>(uniReviewsList, p -> true);
            } else {

                filteredUniReviews = new FilteredList<>(uniReviewsList, filteredUniReviews.getPredicate());
            }
            reviewTable.setItems((ObservableList<Review>) (ObservableList<?>) filteredUniReviews);


        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not load university reviews.");
            e.printStackTrace();
        }
    }

    private void loadBursaryReviews() {
        currentView = "Bursary";
        ChooseMyBursaryReviews.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: #ffffff;");
        ChooseMyUniReviews.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");

        bursaryReviewsList.clear();
        String sql = "SELECT br.BursaryReviewID, br.BursaryID, br.StudentID, br.Content, br.Rating, br.DatePosted " +
                "FROM BursaryReview br WHERE br.StudentID = ?";

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bursaryReviewsList.add(new BursaryReview(
                        rs.getInt("BursaryReviewID"),
                        rs.getInt("BursaryID"),
                        rs.getInt("StudentID"),
                        rs.getString("Content"),
                        rs.getInt("Rating"),
                        rs.getDate("DatePosted").toLocalDate()
                ));
            }

            applySorting();

            if (filteredBursaryReviews == null) {
                filteredBursaryReviews = new FilteredList<>(bursaryReviewsList, p -> true);
            } else {
                filteredBursaryReviews = new FilteredList<>(bursaryReviewsList, filteredBursaryReviews.getPredicate());
            }
            reviewTable.setItems((ObservableList<Review>) (ObservableList<?>) filteredBursaryReviews);


        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not load bursary reviews.");
            e.printStackTrace();
        }
    }

    private TableCell<Review, Void> createActionCell() {
        return new TableCell<>() {
            private final Button editButton = createIconButton("/images/icons/edit_icon.png");
            private final Button deleteButton = createIconButton("/images/icons/delete_icon.png");
            private final HBox hbox = new HBox(8, editButton, deleteButton);

            {
                hbox.setStyle("-fx-alignment: center;");

                editButton.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        Review r = getTableView().getItems().get(getIndex());
                        if (r instanceof UniversityReview uni) openEditor(uni);
                        if (r instanceof BursaryReview burs) openEditor(burs);
                    }
                });

                deleteButton.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        Review r = getTableView().getItems().get(getIndex());
                        boolean confirmed = AlertUtil.showCustomConfirmation(
                                "Delete Review",
                                "Are you sure you want to delete this review?",
                                "Once deleted, it cannot be recovered.",
                                "Delete", "Cancel"
                        );
                        if (confirmed) {
                            if (r instanceof UniversityReview uni) deleteReview(uni.getUniReviewID(), "University");
                            if (r instanceof BursaryReview burs) deleteReview(burs.getBursaryReviewID(), "Bursary");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        };
    }

    private void deleteReview(int reviewId, String type) {
        String sql = type.equals("Bursary")
                ? "DELETE FROM BursaryReview WHERE BursaryReviewID = ?"
                : "DELETE FROM UniversityReview WHERE UniReviewID = ?";
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reviewId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                if (type.equals("University")) {
                    loadUniversityReviews();
                } else {
                    loadBursaryReviews();
                }
                AlertUtil.showInfo("Success", "Review deleted successfully.");
            } else {
                AlertUtil.showWarning("Deletion Failed", "Could not find the review to delete.");
            }

        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not delete the review.");
            e.printStackTrace();
        }
    }

    private void updateRatingInDatabase(Review review, int newRating) {
        String sql;
        int reviewId = review.getId();

        if (review instanceof UniversityReview) {
            sql = "UPDATE UniversityReview SET Rating = ? WHERE UniReviewID = ?";
        } else if (review instanceof BursaryReview) {
            sql = "UPDATE BursaryReview SET Rating = ? WHERE BursaryReviewID = ?";
        } else {
            System.err.println("Unknown review type in updateRatingInDatabase");
            return;
        }

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newRating);
            pstmt.setInt(2, reviewId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Rating updated successfully for review ID: " + reviewId);
            } else {
                System.err.println("Failed to update rating for review ID: " + reviewId);
            }

        } catch (SQLException e) {
            System.err.println("Database error while updating rating for review ID: " + reviewId);
            e.printStackTrace();
            AlertUtil.showError("Save Error", "Could not save the new rating due to a database error.");
        }
    }


    @FXML
    private void handleBackClick() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML
    private void onHandleWriteReviewButton() {
        SceneManager.switchTo("/view/write_reviews.fxml");
    }

    @FXML
    private void onHandleChooseMyUniReviews() {
        loadUniversityReviews();
    }

    @FXML
    private void onHandleChooseMyBursaryReviews() {
        loadBursaryReviews();
    }

    private Button createIconButton(String resourcePath) {
        try {
            Image img = new Image(getClass().getResourceAsStream(resourcePath));
            if (img.isError()) {
                throw new IOException("Failed to load image resource: " + resourcePath);
            }
            ImageView icon = new ImageView(img);
            icon.setFitWidth(20);
            icon.setFitHeight(20);
            Button button = new Button();
            button.setGraphic(icon);
            button.setStyle("-fx-background-color: transparent; -fx-padding: 1;"); // Minimal padding
            button.setCursor(Cursor.HAND);
            return button;
        } catch (Exception e) {
            System.err.println("Error creating icon button for path: " + resourcePath + " - " + e.getMessage());
            Button fallback = new Button("?");
            fallback.setMinWidth(25);
            return fallback;
        }
    }

    private void openEditor(UniversityReview review) {
        SceneManager.switchTo("/view/update_review.fxml",
                (controller.UpdateReviewController c) ->
                        c.initData(review.getUniReviewID(), review.getContent(), "University"));
    }

    private void openEditor(BursaryReview review) {
        SceneManager.switchTo("/view/update_review.fxml",
                (controller.UpdateReviewController c) ->
                        c.initData(review.getBursaryReviewID(), review.getContent(), "Bursary"));
    }

    @FXML private void handleHome() { SceneManager.switchTo("/view/dashboard.fxml"); }
    @FXML private void handleViewProfile() { SceneManager.switchTo("/view/view_profile.fxml"); }
    @FXML private void handleLogout() {
        UserSession.getInstance().logout();
        AlertUtil.showInfo("Logout", "You have been logged out.");
        SceneManager.switchTo("/view/dashboard.fxml");}
}
