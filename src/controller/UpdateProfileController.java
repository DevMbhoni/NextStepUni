package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.University;
import model.UserSession;
import org.mindrot.jbcrypt.BCrypt;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;


public class UpdateProfileController extends BaseController implements Initializable {

    @FXML private TextField fnameTxt;
    @FXML private TextField lnameTxt;
    @FXML private TextField emailTxt;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<University> universityComboBox;
    @FXML private Label proofFileNameLabel;
    @FXML private Button importProofBtn;

    @FXML private PasswordField currentPasswordTxt;
    @FXML private PasswordField newPasswordTxt;
    @FXML private PasswordField confirmPasswordTxt;

    private int studentId;
    private final ObservableList<University> universityList = FXCollections.observableArrayList();
    private int originalUniversityID;
    private String originalProofPath;
    private boolean isCurrentlyVerified;
    private File newProofOfRegFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHeader();
        studentId = UserSession.getInstance().getId();
        if (studentId <= 0) {
            AlertUtil.showError("Session Error", "Invalid student ID found. Please log in again.");
            SceneManager.switchTo("/view/login.fxml");
            return;
        }

        loadUniversityList();
        loadCurrentStudentData();
    }

    private void loadUniversityList() {
        universityList.clear();
        String sql = "SELECT UniversityID, UniName FROM University ORDER BY UniName ASC"; // Added ORDER BY
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                universityList.add(new University(
                        rs.getInt("UniversityID"),
                        rs.getString("UniName")
                ));
            }
            universityComboBox.setItems(universityList);

            universityComboBox.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(University university) {
                    return university == null ? "Select University" : university.getUniName();
                }

                @Override
                public University fromString(String string) {
                    return universityList.stream()
                            .filter(uni -> uni.getUniName() != null && uni.getUniName().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Could not load university list.");
        } catch (NullPointerException e) {
            AlertUtil.showError("Connection Error", "Failed to get database connection.");
            e.printStackTrace();
        }
    }

    private void loadCurrentStudentData() {
        String query = "SELECT s.StuFName, s.StuLName, s.EmailAddress, s.StudDob, s.UniversityID, s.ProofOfReg, s.IsVerified " +
                "FROM Student s " +
                "WHERE s.StudentID = ?";

        try (Connection connection = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                fnameTxt.setText(rs.getString("StuFName"));
                lnameTxt.setText(rs.getString("StuLName"));
                emailTxt.setText(rs.getString("EmailAddress"));
                java.sql.Date sqlDate = rs.getDate("StudDob");
                if (sqlDate != null) {
                    dobPicker.setValue(sqlDate.toLocalDate());
                } else {
                    dobPicker.setValue(null);
                }


                originalUniversityID = rs.getInt("UniversityID");
                originalProofPath = rs.getString("ProofOfReg");
                isCurrentlyVerified = rs.getBoolean("IsVerified");

                proofFileNameLabel.setText(originalProofPath != null && !originalProofPath.isEmpty()
                        ? getFileNameFromPath(originalProofPath) // Show just the filename
                        : "No file uploaded.");

                if (originalUniversityID > 0) {
                    universityList.stream()
                            .filter(uni -> uni.getUniversityID() == originalUniversityID)
                            .findFirst()
                            .ifPresent(universityComboBox::setValue);
                } else {
                    universityComboBox.setValue(null);
                }
            } else {
                AlertUtil.showError("Profile Error", "Could not find profile data for your account.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Unexpected Error", "Could not load profile data.");
        } catch (NullPointerException e) {
            AlertUtil.showError("Connection Error", "Failed to connection.Please try again");
            e.printStackTrace();
        }
    }

    private String getFileNameFromPath(String path) {
        if (path == null) return "";

        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (lastSeparator >= 0) ? path.substring(lastSeparator + 1) : path;
    }


    @FXML
    private void handleImportProof() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select New Proof of Registration");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image and PDF Files", "*.png", "*.jpg", "*.jpeg", "*.pdf")
        );
        Stage stage = (Stage) importProofBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            this.newProofOfRegFile = selectedFile;
            proofFileNameLabel.setText(newProofOfRegFile.getName());
        }
    }

    @FXML
    private void handleSaveChanges() {
        University selectedUniversity = universityComboBox.getValue();
        if (fnameTxt.getText().isBlank() || lnameTxt.getText().isBlank() || emailTxt.getText().isBlank() || dobPicker.getValue() == null || selectedUniversity == null) {
            AlertUtil.showError("Invalid Input", "Please fill in all required personal information fields (First Name, Last Name, Email, DOB, University).");
            return;
        }
        if(dobPicker.getValue().isAfter(LocalDate.of(2007, 12, 31))) {
            AlertUtil.showError("Invalid input","Please enter a valid date of birth (must be before 2008)");
            return;
        }

        boolean universityChanged = selectedUniversity.getUniversityID() != originalUniversityID;
        boolean proofChanged = newProofOfRegFile != null;
        boolean needsReVerification = isCurrentlyVerified && (universityChanged || proofChanged);
        boolean proceedWithSave = true;

        if (needsReVerification) {
            proceedWithSave = AlertUtil.showCustomConfirmation(
                    "Verification Reset Confirmation",
                    "Change Requires Re-verification",
                    "Changing your university or uploading new proof will reset your 'Verified' status.\nYour account will need administrator approval again.\n\nDo you want to proceed with these changes?",
                    "Proceed & Reset Verification",
                    "Cancel Change"
            );
        }

        if (!proceedWithSave) {
            return;
        }
        String newHashedPassword = null;
        boolean isUpdatingPassword = !newPasswordTxt.getText().isEmpty() || !currentPasswordTxt.getText().isEmpty() || !confirmPasswordTxt.getText().isEmpty();

        if (isUpdatingPassword) {
            String currentPassword = currentPasswordTxt.getText();
            String newPassword = newPasswordTxt.getText();
            String confirmPassword = confirmPasswordTxt.getText();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                AlertUtil.showError("Password Error", "To change your password, please fill in Current, New, and Confirm Password fields.");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                AlertUtil.showError("Password Error", "Your New and Confirm Passwords do not match.");
                return;
            }
            if (newPassword.length() < 8) {
                AlertUtil.showError("Password Error", "New password must be at least 8 characters long.");
                return;
            }
            if (!verifyCurrentPassword(currentPassword)) {
                AlertUtil.showError("Password Error", "Your Current Password input is incorrect.");
                return;
            }

            newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        }

        Connection conn = null;
        boolean updateSuccess = false;
        String finalProofPath = originalProofPath;

        try {
            conn = DatabaseConnector.getInstance().getConnection();
            if (proofChanged && originalProofPath != null && !originalProofPath.isEmpty()) {
                try {
                    Path oldProof = Paths.get("student_documents").resolve(originalProofPath);
                    Files.deleteIfExists(oldProof);
                    System.out.println("Deleted old proof file: " + oldProof.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Could not delete old proof file: " + originalProofPath + " - " + e.getMessage());
                }
            }
            if (proofChanged) {
                finalProofPath = saveProofOfReg(newProofOfRegFile, fnameTxt.getText() + "_" + lnameTxt.getText());
            }

            StringBuilder sql = new StringBuilder(
                    "UPDATE Student SET StuFName = ?, StuLName = ?, EmailAddress = ?, StudDob = ?, UniversityID = ?, ProofOfReg = ?, IsVerified = ?");
            if (isUpdatingPassword) {
                sql.append(",Password = ?");
            }
            sql.append(" WHERE StudentID = ?");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                boolean newVerifiedStatus = needsReVerification ? false : isCurrentlyVerified;

                // Set parameters
                pstmt.setString(1, fnameTxt.getText().trim());
                pstmt.setString(2, lnameTxt.getText().trim());
                pstmt.setString(3, emailTxt.getText().trim());
                pstmt.setDate(4, java.sql.Date.valueOf(dobPicker.getValue()));
                pstmt.setInt(5, selectedUniversity.getUniversityID());
                pstmt.setString(6, finalProofPath);
                pstmt.setBoolean(7, newVerifiedStatus);

                int paramIndex = 8;
                if (isUpdatingPassword) {
                    pstmt.setString(paramIndex++, newHashedPassword);
                }
                pstmt.setInt(paramIndex, studentId);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    updateSuccess = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Unexpected Error", "An error occurred while updating your profile!");
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("File Error", "Could not save your new proof of registration file!");
        } catch (NullPointerException e) {
            e.printStackTrace();
            AlertUtil.showError("Connection Error", "Failed to connection. Please try again.");
        }


        if (updateSuccess) {
            AlertUtil.showInfo("Success", "Your profile has been updated successfully.");
            if (needsReVerification) {
                UserSession.getInstance().logout();
                AlertUtil.showInfo("Verification Required", "Your account now requires administrator verification due to the changes made.");
                SceneManager.switchTo("/view/dashboard.fxml");
            } else {
                SceneManager.switchTo("/view/view_profile.fxml");
            }
        } else {
            loadCurrentStudentData();
        }
    }


    private boolean verifyCurrentPassword(String clearTextPassword) {
        String sql = "SELECT Password FROM Student WHERE StudentID = ?";
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("Password");
                if (hashedPassword == null || hashedPassword.isEmpty()) {
                    return clearTextPassword.isEmpty();
                }

                return BCrypt.checkpw(clearTextPassword, hashedPassword);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Could not verify current password.");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid password hash format in database for student ID: " + studentId);
            AlertUtil.showError("Security Error", "Account data integrity issue. Cannot verify password.");
        } catch (NullPointerException e) {
            AlertUtil.showError("Connection Error", "Failed to get database connection for password check.");
            e.printStackTrace();
        }
        return false;
    }


    @FXML
    private void handleCancel() {
        SceneManager.switchTo("/view/view_profile.fxml");
    }

    private String saveProofOfReg(File sourceFile, String studentName) throws IOException {
        Path docDir = Paths.get("student_documents");
        if (!Files.exists(docDir)) {
            Files.createDirectories(docDir);
        }

        String sanitizedName = studentName.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
        String fileExtension = getFileExtension(sourceFile.getName());
        String newFileName = studentId + "_" + sanitizedName + "_proof_" + System.currentTimeMillis() + "." + fileExtension;

        Path destinationFile = docDir.resolve(newFileName);

        Files.copy(sourceFile.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        return newFileName;
    }

    private String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot == -1 || lastIndexOfDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastIndexOfDot + 1).toLowerCase();
    }

    @FXML private void handleHome() { SceneManager.switchTo("/view/dashboard.fxml"); }
    @FXML private void handleViewProfile() { SceneManager.switchTo("/view/view_profile.fxml"); }
    @FXML private void handleLogout() {
        UserSession.getInstance().logout();
        AlertUtil.showInfo("Logout", "You have been logged out.");
        SceneManager.switchTo("/view/dashboard.fxml");
    }
}

