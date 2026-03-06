package controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import util.DatabaseConnector;
import util.SceneManager;
import util.EmailService;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Random;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Button sendCodeButton;
    @FXML
    private void handleSendCode() {
        String email = emailField.getText();
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Email field cannot be empty.");
            return;
        }


        Task<String> sendCodeTask = new Task<>() {
            @Override
            protected String call() throws Exception {

                if (!userExists(email)) {
                    throw new Exception("No account found with that email address.");
                }

                String resetCode = generateResetCode();
                if (saveResetCode(email, resetCode)) {
                    EmailService.sendPasswordResetEmail(email, resetCode);
                    System.out.println("Password Reset Code for " + email + " is: " + resetCode);
                    return resetCode;
                } else {
                    throw new Exception("Could not generate a reset code. Please try again.");
                }
            }
        };

        sendCodeTask.setOnRunning(event -> {
            loadingIndicator.setVisible(true);
            sendCodeButton.setDisable(true);
        });

        sendCodeTask.setOnSucceeded(event -> {
            loadingIndicator.setVisible(false);
            sendCodeButton.setDisable(false);
            showAlert(Alert.AlertType.INFORMATION, "Code Sent", "A reset code has been sent to: " + email);
            switchToResetPasswordScreen(email);
        });
        sendCodeTask.setOnFailed(event -> {
            loadingIndicator.setVisible(false);
            sendCodeButton.setDisable(false);
            Throwable e = sendCodeTask.getException();
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            e.printStackTrace();
        });

        new Thread(sendCodeTask).start();
    }

    private void switchToResetPasswordScreen(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/reset_password.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ResetPasswordController) {
                ResetPasswordController rpc = (ResetPasswordController) controller;
                rpc.setEmail(email);
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Reset Password screen.");
        }
    }
    @FXML
    private void handleGoBack() {
        SceneManager.switchTo("/view/login_view.fxml");
    }

    private boolean userExists(String email) throws SQLException{
        String sql = "SELECT StudentID FROM Student WHERE EmailAddress = ?";
        try (PreparedStatement pstmt = DatabaseConnector.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    private String generateResetCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private boolean saveResetCode(String email, String code) {
        String sql = "UPDATE Student SET ResetCode = ?, ResetCodeTimestamp = ? WHERE EmailAddress = ?";
        try (PreparedStatement pstmt = DatabaseConnector.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(3, email);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}