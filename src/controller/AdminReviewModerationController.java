package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import model.UserSession;
import util.AlertUtil;
import util.DatabaseConnector;
import util.EmailService;
import util.SceneManager;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminReviewModerationController extends BaseController implements Initializable {

    @FXML private StackPane rootStackPane;
    @FXML private VBox mainContentVBox;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private TextField searchField;
    @FXML private TableView<ModerationReviewWrapper> reviewsTable;
    @FXML private TableColumn<ModerationReviewWrapper, String> typeColumn;
    @FXML private TableColumn<ModerationReviewWrapper, String> authorColumn;
    @FXML private TableColumn<ModerationReviewWrapper, String> contentColumn;
    @FXML private TableColumn<ModerationReviewWrapper, Integer> ratingColumn;
    @FXML private TableColumn<ModerationReviewWrapper, LocalDate> dateColumn;
    @FXML private TableColumn<ModerationReviewWrapper, Void> actionColumn;

    private ObservableList<ModerationReviewWrapper> allReviewsList = FXCollections.observableArrayList();
    private FilteredList<ModerationReviewWrapper> filteredReviewsList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        if (UserSession.getInstance().getRole() != UserSession.UserRole.ADMIN) {
            AlertUtil.showError("Access Denied", "You do not have permission to view this page.");
            javafx.application.Platform.runLater(() -> SceneManager.switchTo("/view/dashboard.fxml"));
            return;
        }

        configureTableColumns();
        loadAllReviews();
        setupSearchAndFilters();

        showLoading(false);
    }

    private void configureTableColumns() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("reviewType"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("studentEmail"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("datePosted"));
        actionColumn.setCellFactory(createActionCellFactory());

        typeColumn.setSortable(false);
        authorColumn.setSortable(false);
        contentColumn.setSortable(false);
        ratingColumn.setSortable(false);
        dateColumn.setSortable(false);
        actionColumn.setSortable(false);
    }


    private void setupSearchAndFilters() {
        filteredReviewsList = new FilteredList<>(allReviewsList, p -> true);
        reviewsTable.setItems(filteredReviewsList);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredReviewsList.setPredicate(review -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase().trim();
                return (review.getContent() != null && review.getContent().toLowerCase().contains(lowerCaseFilter))
                        || (review.getStudentEmail() != null && review.getStudentEmail().toLowerCase().contains(lowerCaseFilter))
                        || (review.getReviewType() != null && review.getReviewType().toLowerCase().contains(lowerCaseFilter));
            });
        });
    }

    private void loadAllReviews() {
        allReviewsList.clear();
        Connection conn = DatabaseConnector.getInstance().getConnection();

        String uniSql = "SELECT r.UniReviewID, r.StudentID, r.Content, r.Rating, r.DatePosted, s.EmailAddress " +
                "FROM UniversityReview r " +
                "JOIN Student s ON r.StudentID = s.StudentID";
        try (PreparedStatement pstmt = conn.prepareStatement(uniSql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Date sqlDate = rs.getDate("DatePosted");
                LocalDate date = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                allReviewsList.add(new ModerationReviewWrapper(
                        rs.getInt("UniReviewID"),
                        "University",
                        rs.getInt("StudentID"),
                        rs.getString("EmailAddress"),
                        rs.getString("Content"),
                        rs.getInt("Rating"),
                        date
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Failed to load university reviews.");
        }

        String bursarySql = "SELECT r.BursaryReviewID, r.StudentID, r.Content, r.Rating, r.DatePosted, s.EmailAddress " +
                "FROM BursaryReview r " +
                "JOIN Student s ON r.StudentID = s.StudentID";
        try (PreparedStatement pstmt = conn.prepareStatement(bursarySql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {

                Date sqlDate = rs.getDate("DatePosted");
                LocalDate date = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                allReviewsList.add(new ModerationReviewWrapper(
                        rs.getInt("BursaryReviewID"),
                        "Bursary",
                        rs.getInt("StudentID"),
                        rs.getString("EmailAddress"),
                        rs.getString("Content"),
                        rs.getInt("Rating"),
                        date
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Failed to load bursary reviews.");
        }
        if (filteredReviewsList != null) {
            filteredReviewsList.setPredicate(p -> true);
        }
        reviewsTable.refresh();
    }


    private Callback<TableColumn<ModerationReviewWrapper, Void>, TableCell<ModerationReviewWrapper, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");

            {
                deleteButton.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: white; -fx-font-weight: bold;");
                deleteButton.setOnAction(event -> {
                    // Check bounds before getting item
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ModerationReviewWrapper review = getTableView().getItems().get(getIndex());
                        handleDeleteReview(review, deleteButton);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }


    private void handleDeleteReview(ModerationReviewWrapper review, Button deleteBtn) {

        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Confirm Deletion");
        reasonDialog.setHeaderText("Delete Review by " + review.getStudentEmail());
        reasonDialog.setContentText("Please provide a brief reason for deleting this review (this will be emailed to the user):");

        Button okReasonButton = (Button) reasonDialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okReasonButton != null) {
            okReasonButton.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: white; -fx-font-weight: bold;");
        }

        Optional<String> reasonResult = reasonDialog.showAndWait();

        if (!reasonResult.isPresent() || reasonResult.get().trim().isEmpty()) {
            if (reasonResult.isPresent()) {
                AlertUtil.showWarning("Reason Required", "You must provide a reason to delete a review.");
            }
            return;
        }
        String reason = reasonResult.get().trim();



        boolean confirmed = AlertUtil.showCustomConfirmation(
                "Final Confirmation",
                "Permanently delete this review?",
                "Reason: " + reason + "\n\nThe user (" + review.getStudentEmail() + ") will be notified via email.",
                "Delete Review",
                "Cancel"
        );

        if (!confirmed) {
            return;
        }

        showLoading(true);
        deleteBtn.setDisable(true);

        Task<Boolean> deleteTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                boolean dbDeleted = deleteReviewFromDatabase(review);
                if (!dbDeleted) {
                    throw new SQLException("Failed to delete review from database.");
                }

                String emailBody = "Hello,\n\n"
                        + "Your review for a " + review.getReviewType() + " posted on " + review.getDatePosted() + " has been removed by an administrator.\n\n"
                        + "Reason: " + reason + "\n\n"
                        + "Original Review Content:\n\"" + review.getContent() + "\"\n\n"
                        + "If you believe this was in error, please contact support.\n\n"
                        + "Thanks,\nThe NextStepUni Team";

                boolean emailSent = EmailService.rejectMessage("NextStepUni review deleted",review.getStudentEmail(), emailBody);
                if (!emailSent) {
                    System.err.println("Database delete successful, but failed to send notification email to " + review.getStudentEmail());

                }
                return true;
            }
        };

        deleteTask.setOnSucceeded(e -> {
            showLoading(false);
            allReviewsList.remove(review);
            reviewsTable.refresh();
            AlertUtil.showInfo("Review Deleted", "The review has been deleted and the user has been notified.");
        });

        deleteTask.setOnFailed(e -> {
            showLoading(false);
            deleteBtn.setDisable(false);
            Throwable ex = deleteTask.getException();
            ex.printStackTrace();
            if (ex instanceof SQLException) {
                AlertUtil.showError("Database Error", "Failed to delete the review from the database.");
            } else {
                AlertUtil.showError("Operation Failed", "Could not delete the review: " + ex.getMessage());
            }
        });

        new Thread(deleteTask).start();
    }

    private boolean deleteReviewFromDatabase(ModerationReviewWrapper review) {
        String sql;
        if (review.getReviewType().equals("University")) {
            sql = "DELETE FROM UniversityReview WHERE UniReviewID = ?";
        } else if (review.getReviewType().equals("Bursary")) {
            sql = "DELETE FROM BursaryReview WHERE BursaryReviewID = ?";
        } else {
            System.err.println("Unknown review type for deletion: " + review.getReviewType());
            return false;
        }

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, review.getReviewId());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleFilterAll() {
        filteredReviewsList.setPredicate(p -> true); // Show all
    }

    @FXML
    private void handleFilterUniversity() {
        filteredReviewsList.setPredicate(p -> "University".equals(p.getReviewType()));
    }

    @FXML
    private void handleFilterBursary() {
        filteredReviewsList.setPredicate(p -> "Bursary".equals(p.getReviewType()));
    }

    private void showLoading(boolean isLoading) {
        if (loadingIndicator != null) loadingIndicator.setVisible(isLoading);
        if (mainContentVBox != null) mainContentVBox.setDisable(isLoading);

        if (searchField != null) searchField.setDisable(isLoading);

    }


    @FXML
    private void handleGoBack() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML private void handleViewProfile() { SceneManager.switchTo("/view/view_profile.fxml"); }
    @FXML private void handleLogout() {
        UserSession.getInstance().logout();
        AlertUtil.showInfo("Logout", "You have been logged out.");
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    public static class ModerationReviewWrapper {
        private int reviewId;
        private String reviewType;
        private int studentId;
        private String studentEmail;
        private String content;
        private int rating;
        private LocalDate datePosted;

        public ModerationReviewWrapper(int reviewId, String reviewType, int studentId, String studentEmail, String content, int rating, LocalDate datePosted) {
            this.reviewId = reviewId;
            this.reviewType = reviewType;
            this.studentId = studentId;
            this.studentEmail = studentEmail;
            this.content = content;
            this.rating = rating;
            this.datePosted = datePosted;
        }

        public int getReviewId() { return reviewId; }
        public String getReviewType() { return reviewType; }
        public int getStudentId() { return studentId; }
        public String getStudentEmail() { return studentEmail; }
        public String getContent() { return content; }
        public int getRating() { return rating; }
        public LocalDate getDatePosted() { return datePosted; }
    }
}

