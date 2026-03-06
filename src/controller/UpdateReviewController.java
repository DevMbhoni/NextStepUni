package controller;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import model.BursaryReview;
import model.Review;
import model.UniversityReview;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;


public class UpdateReviewController extends BaseController implements Initializable {

    @FXML private TextArea reviewTextArea;
    @FXML private Button submitBtn;

    private int reviewID;
    private String reviewType;
    private String initialContent;
    private Review currentReview;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
    }


    public void initData(int reviewID, String content, String type) {
        this.reviewID = reviewID;
        this.reviewType = type;
        this.initialContent = content;
        reviewTextArea.setText(content);

        if ("University".equalsIgnoreCase(type)) {
            currentReview = new UniversityReview(reviewID, content, 0, null);
        } else if ("Bursary".equalsIgnoreCase(type)) {
            currentReview = new BursaryReview(reviewID, 0, content, null, 0);
        } else {
            System.err.println("Unknown review type passed to initData: " + type);
            AlertUtil.showError("Initialization Error", "Cannot edit review of unknown type.");

            submitBtn.setDisable(true);
        }
    }

    @FXML
    private void handleSave() {
        String updatedText = reviewTextArea.getText().trim();
        if (updatedText.isEmpty()) {
            AlertUtil.showWarning( "Empty Review", "Please write something before saving.");
            return;
        }

         if (updatedText.equals(initialContent)) {
             AlertUtil.showInfo("No Changes", "No changes were made to the review.");
             SceneManager.switchTo("/view/my_reviews.fxml");
             return;
         }

        try (Connection conn = DatabaseConnector.getInstance().getConnection()) {
            String sql;
            String contentColumnName = "Content";

            if ("University".equalsIgnoreCase(reviewType)) {
                sql = "UPDATE UniversityReview SET " + contentColumnName + " = ? WHERE UniReviewID = ?";
            } else if ("Bursary".equalsIgnoreCase(reviewType)) {
                sql = "UPDATE BursaryReview SET " + contentColumnName + " = ? WHERE BursaryReviewID = ?";
            } else {
                AlertUtil.showError("Error", "Cannot save review: Unknown review type.");
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, updatedText);
                stmt.setInt(2, this.reviewID);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    AlertUtil.showInfo( "Success", "Review updated successfully!");
                    SceneManager.switchTo("/view/my_reviews.fxml");
                } else {
                    AlertUtil.showError("Update Failed", "Could not find the review to update in the database.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError( "Database Error", "Failed to update review. Please try again.");
        } catch (NullPointerException e) {
            AlertUtil.showError("Database Error", "Failed to get database connection.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        if (!reviewTextArea.getText().trim().equals(initialContent.trim())) {
            boolean confirm = AlertUtil.showCustomConfirmation(
                    "Confirm Cancellation",
                    "You have unsaved changes.",
                    "Discard changes and go back?",
                    "Discard", "Keep Editing"
            );
            if (!confirm) {
                return;
            }
        }
        SceneManager.switchTo("/view/my_reviews.fxml");
    }


    @FXML
    private void handleBackClick() {
         handleCancel();
    }

     @FXML private void handleHome() { SceneManager.switchTo("/view/dashboard.fxml"); }
     @FXML private void handleViewProfile() { SceneManager.switchTo("/view/view_profile.fxml"); }
     @FXML private void handleLogout() { }
     @FXML private void handleLoginRegister() {  }
}
