package controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;
import util.AlertUtil;
import util.DatabaseConnector;

import util.SceneManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

public class ResetPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField newPasswordFieldText;
    @FXML private TextField confirmPasswordFieldText;
    @FXML private CheckBox showPasswordCheckBox;

    public void initialize() {

        newPasswordFieldText.textProperty().bindBidirectional(newPasswordField.textProperty());
        confirmPasswordFieldText.textProperty().bindBidirectional(confirmPasswordField.textProperty());
    }

    public void setEmail(String email) {
        emailField.setText(email);
        emailField.setEditable(false);
    }

    @FXML
    private void handleResetPassword() {
        String email = emailField.getText();
        String code = codeField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (email.isEmpty() || code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            AlertUtil.showError(  "Error", "All fields are required.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            AlertUtil.showError(  "Error", "Passwords do not match.");
            return;
        }
        if (newPassword.length() < 8) {
            AlertUtil.showError(  "Error", "Password must be at least 8 characters.");
            return;
        }

        if (isCodeValid(email, code)) {
            if (updatePassword(email, newPassword)) {
                AlertUtil.showInfo(  "Success", "Your password has been reset successfully. Please log in.");
                SceneManager.switchTo("/view/login_view.fxml");
            } else {
                AlertUtil.showError(  "Error", "Failed to update password.");
            }
        } else {
            AlertUtil.showError( "Error", "Invalid or expired reset code.");
        }
    }

    private boolean isCodeValid(String email, String code) {
        String sql = "SELECT ResetCode, ResetCodeTimestamp FROM Student WHERE EmailAddress = ?";
        try (Connection conn = DatabaseConnector.getInstance().getConnection(); // Use DatabaseManager
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbCode = rs.getString("ResetCode");
                Timestamp dbTimestamp = rs.getTimestamp("ResetCodeTimestamp");

                if (dbCode == null || dbTimestamp == null) return false;

                Instant now = Instant.now();
                Instant codeTime = dbTimestamp.toInstant();
                long minutesElapsed = Duration.between(codeTime, now).toMinutes();

                return dbCode.equals(code) && minutesElapsed <= 15;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean updatePassword(String email, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "UPDATE Student SET Password = ?, ResetCode = NULL, ResetCodeTimestamp = NULL WHERE EmailAddress = ?";
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleShowPassword() {
        boolean isSelected = showPasswordCheckBox.isSelected();
        newPasswordField.setVisible(!isSelected);
        newPasswordField.setManaged(!isSelected);
        confirmPasswordField.setVisible(!isSelected);
        confirmPasswordField.setManaged(!isSelected);

        newPasswordFieldText.setVisible(isSelected);
        newPasswordFieldText.setManaged(isSelected);
        confirmPasswordFieldText.setVisible(isSelected);
        confirmPasswordFieldText.setManaged(isSelected);
    }

    @FXML
    private void handleCancel() {
        SceneManager.switchTo("/view/login_view.fxml");
    }


}
