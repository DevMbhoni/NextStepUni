package controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import model.Bursary;
import model.UserSession;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class WriteBursaryReviewController extends BaseController implements Initializable {

    private final int studentID = UserSession.getInstance().getId();
    @FXML
    private Button submitBtn;
    private Bursary bursary;
    @FXML
    private TextArea TextAreaC100;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
    }
    public void initData(Bursary bur){
        bursary = bur;
    }

    @FXML
    private void handleSave() {
        String reviewText = TextAreaC100.getText().trim();
        if (reviewText.isEmpty()) {
            AlertUtil.showWarning("Empty Review", "Please write something before submitting.");
            return;
        }

        boolean confirmed = AlertUtil.showConfirmation("Submit Review",
                "Are you sure you want to submit this review?\nOnce submitted it will be visible to other users.");

        if (confirmed) {
            submitReview(reviewText);
        }
    }

    @FXML
    private void submitReview(String content) {
        LocalDate datePosted = LocalDate.now();

        try (Connection conn = DatabaseConnector.getInstance().getConnection()) {
            String insertSQL = "INSERT INTO BursaryReview (BursaryID, StudentID, Content, Rating, DatePosted) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(insertSQL);
            stmt.setInt(1, bursary.getBursaryID());
            stmt.setInt(2, studentID);
            stmt.setString(3, content);
            stmt.setInt(4, 0);
            stmt.setDate(5, Date.valueOf(datePosted));
            int affectedRows = stmt.executeUpdate();
            if(affectedRows > 0)
            {
                AlertUtil.showInfo("Successfully", "Review submitted successfully.");
                SceneManager.switchTo("/view/write_reviews.fxml");

            } else {
                AlertUtil.showError("Unexpected Error", "Unexpected error occurred. Please try again.");
            }


        } catch (SQLException e) {
            AlertUtil.showError("Unexpected Error", "Unexpected error occurred. Please try again.");
            throw new RuntimeException(e);
        }

    }
    @FXML
    private void handleBackClick(){
        boolean confirm = AlertUtil.showConfirmation("Confirm Cancellation", "Any unsaved changes will be lost. Are you sure?");
        if (confirm) {
            SceneManager.switchTo("/view/select_bursary.fxml");
        }
    }
    @FXML
    public void handleCancel(Event event) {
        boolean confirm = AlertUtil.showConfirmation("Confirm Cancellation", "Any unsaved changes will be lost. Are you sure?");
        if (confirm) {
            SceneManager.switchTo("/view/select_bursary.fxml");
        }
    }
}
