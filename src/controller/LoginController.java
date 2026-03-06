package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.UserSession;
import org.mindrot.jbcrypt.BCrypt;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class LoginController {

    @FXML private ToggleGroup roleGroup;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Button loginButton;


    @FXML private RadioButton adminRadio;
    @FXML private RadioButton studentRadio;

    @FXML
    public void initialize() {

        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        studentRadio.setSelected(true);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        String selectedRole = adminRadio.isSelected() ? "Admin" : "Student";

        if (email.isEmpty() || password.isEmpty()) {
            AlertUtil.showError("Login Failed", "Email and password fields are required.");
            return;
        }


        if (validateCredentials(email, password, selectedRole)) {
            SceneManager.switchTo("/view/dashboard.fxml");
        } else {
            AlertUtil.showError("Login Failed", "Invalid credentials for selected role.");
        }
    }

    @FXML
    private void handleContinueAsGuest() {
        UserSession.getInstance().login(0, "Guest", UserSession.UserRole.GUEST);
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    private boolean validateCredentials(String email, String password, String role) {
        String sql;
        boolean isAdmin = role.equals("Admin");
        String idCol, nameCol, passCol, emailCol;

        if (isAdmin) {
            sql = "SELECT AdminID, UserName,Password FROM Admin WHERE UserName = ?";
            idCol = "AdminID";
            nameCol = "UserName";
            passCol = "Password";
            emailCol = "UserName";
        } else {
            sql = "SELECT StudentID, StuFName, Password FROM Student WHERE EmailAddress = ?";
            idCol = "StudentID";
            nameCol = "StuFName";
            passCol = "Password";
            emailCol = "EmailAddress";
        }

        Connection conn = DatabaseConnector.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                String storedPassword = rs.getString(passCol);

                if (storedPassword == null) {
                    return false;
                }

                boolean passwordMatches = false;


                if (isAdmin) {

                    passwordMatches = password.equals(storedPassword);
                } else {

                    try {
                        passwordMatches = BCrypt.checkpw(password, storedPassword);
                    } catch (IllegalArgumentException e) {
                        // This catches if the student password is not a valid hash
                        System.err.println("Login error: Invalid hash format in database for student " + email);
                        AlertUtil.showError("Login Error", "An account data error occurred. Please contact support.");
                        return false; // Stop login
                    }
                }

                if (passwordMatches) {

                    int id = rs.getInt(idCol);
                    String name = rs.getString(nameCol);
                    UserSession.UserRole userRole = isAdmin ? UserSession.UserRole.ADMIN : UserSession.UserRole.STUDENT;


                    UserSession.getInstance().login(id, name, userRole);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "An error occurred while trying to log in.");
        }

        return false;
    }

    @FXML
    private void handleShowPassword() {
        boolean isSelected = showPasswordCheckBox.isSelected();
        passwordTextField.setManaged(isSelected);
        passwordTextField.setVisible(isSelected);
        passwordField.setManaged(!isSelected);
        passwordField.setVisible(!isSelected);

        if (isSelected) {
            passwordTextField.setText(passwordField.getText());
        } else {
            passwordField.setText(passwordTextField.getText());
        }
    }

    @FXML
    private void handleRegister() {
        SceneManager.switchTo("/view/register.fxml");
    }

    @FXML
    private void handleResetPassword() {
        SceneManager.switchTo("/view/forgot_password.fxml");
    }


}

