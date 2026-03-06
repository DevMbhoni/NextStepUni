package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import model.University;
import model.UserSession;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.ResourceBundle;

import static controller.AddUniversityController.validateDateAndWebLink;


public class UpdateUniversityController extends BaseController implements Initializable {
    @FXML
    private ImageView universityImageView;
    @FXML
    private TextField universityNameField;
    @FXML
    private TextField locationField;
    @FXML
    private TextField websiteLinkField;
    @FXML
    private DatePicker applicationDeadlinePicker;
    @FXML
    private TextArea descriptionArea;

    private University currentUniversity;

    private File newImageFile;
    private String originalImagePath;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
    }

    public void initData(University university) {
        this.currentUniversity = university;
        this.newImageFile = null;

        this.originalImagePath = university.getUniPicturePath();
        universityNameField.setText(university.getUniName());
        locationField.setText(university.getLocation());
        websiteLinkField.setText(university.getWebsiteLink());
        applicationDeadlinePicker.setValue(university.getApplicationDeadline());
        descriptionArea.setText(university.getDescription());

        loadUniversityImage(university.getUniPicturePath());
    }
    @FXML
    private void handleUpdate() {
        if (!validateInput()) {
            return;
        }

        String updatedName = universityNameField.getText().trim();
        String updatedLocation = locationField.getText().trim();
        String updatedWebsite = websiteLinkField.getText().trim();
        LocalDate updatedDeadline = applicationDeadlinePicker.getValue();
        String updatedDescription = descriptionArea.getText().trim();
        String finalImagePath = originalImagePath;
        boolean imageChanged = newImageFile != null;
        boolean imageRemoved = currentUniversity.getUniPicturePath() == null && originalImagePath != null; // Flag if image was removed

        Path imageDir = Paths.get("university_images");

        Connection conn = null;
        boolean updateSuccess = false;

        try {

            if ((imageChanged || imageRemoved) && originalImagePath != null && !originalImagePath.isEmpty()) {
                try {
                    Path oldImagePath = imageDir.resolve(originalImagePath);
                    Files.deleteIfExists(oldImagePath);
                    System.out.println("Deleted old university image: " + oldImagePath.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Could not delete old image file: " + originalImagePath + " - " + e.getMessage());
                    AlertUtil.showWarning("File Warning", "Could not remove the old image file, but proceeding with update.");
                }
            }

            if (imageChanged) {
                finalImagePath = saveImageToDataDirectory(newImageFile, updatedName);
            } else if (imageRemoved) {
                finalImagePath = null;
            }

            conn = DatabaseConnector.getInstance().getConnection();
            String sql = "UPDATE University SET UniName = ?, Location = ?, WebsiteLink = ?, " +
                    "ApplicationDeadline = ?, Description = ?, UniPicturePath = ? " +
                    "WHERE UniversityID = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, updatedName);
                pstmt.setString(2, updatedLocation);
                pstmt.setString(3, updatedWebsite);
                // Handle null date correctly
                if (updatedDeadline != null) {
                    pstmt.setDate(4, java.sql.Date.valueOf(updatedDeadline));
                } else {
                    pstmt.setNull(4, java.sql.Types.DATE);
                }
                pstmt.setString(5, updatedDescription);
                pstmt.setString(6, finalImagePath);
                pstmt.setInt(7, currentUniversity.getUniversityID());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    updateSuccess = true;
                }
            }
        } catch (SQLException e) {
            AlertUtil.showError("Unexpected Error", "An error occurred while updating the university: " + e.getMessage());
        } catch (IOException e) {
            AlertUtil.showError("File Error", "Could not save the new university image file: " + e.getMessage());
        } catch (NullPointerException e) {
            AlertUtil.showError("Connection Error", "Failed to get database connection.");
        }

        if (updateSuccess) {
            AlertUtil.showInfo("Success", "University '" + updatedName + "' updated successfully.");
            SceneManager.switchTo("/view/manage_universities.fxml");
        } else {
            AlertUtil.showError( "Update Failed", "Could not update the university in the database. Please check the logs.");
        }
    }

    @FXML
    private void handleUpdateImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select New University Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        Stage stage = (Stage) universityImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.newImageFile = file;
            try {
                Image newImage = new Image(file.toURI().toString());
                universityImageView.setImage(newImage);

                currentUniversity.setUniPicturePath(null);
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Image Load Error", "Could not display the selected image.");
                this.newImageFile = null;
                loadUniversityImage(originalImagePath);
            }
        }
    }
    @FXML
    private void handleRemoveImage() {
        universityImageView.setImage(null);
        this.newImageFile = null;
        currentUniversity.setUniPicturePath(null);
    }

    @FXML
    private void handleCancel() {
        boolean changed = !universityNameField.getText().equals(currentUniversity.getUniName())
                || !Objects.equals(locationField.getText(), currentUniversity.getLocation())
                || !Objects.equals(websiteLinkField.getText(), currentUniversity.getWebsiteLink())
                || !Objects.equals(applicationDeadlinePicker.getValue(), currentUniversity.getApplicationDeadline())
                || !Objects.equals(descriptionArea.getText(), currentUniversity.getDescription())
                || newImageFile != null
                || (currentUniversity.getUniPicturePath() == null && originalImagePath != null);

        if (changed) {
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
        SceneManager.switchTo("/view/manage_universities.fxml");
    }

    private void loadUniversityImage(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File imageFile = new File("university_images", imagePath);
                if (imageFile.exists()) {
                    universityImageView.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    System.err.println("Image file not found: " + imageFile.getAbsolutePath());
                    universityImageView.setImage(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                universityImageView.setImage(null);
            }
        } else {
            universityImageView.setImage(null);
        }
    }

    private String saveImageToDataDirectory(File sourceFile, String universityName) throws IOException {
        Path imageDir = Paths.get("university_images");
        if (!Files.exists(imageDir)) {
            Files.createDirectories(imageDir);
        }
        String sanitizedName = universityName.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
        String fileExtension = getFileExtension(sourceFile.getName());
        String newFileName = sanitizedName + "_" + System.currentTimeMillis() + "." + fileExtension;
        Path destinationFile = imageDir.resolve(newFileName);
        Files.copy(sourceFile.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return newFileName;
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        return (lastIndexOf == -1) ? "" : fileName.substring(lastIndexOf + 1);
    }
    private boolean validateInput() {
        LocalDate deadline = applicationDeadlinePicker.getValue();
        String website = websiteLinkField.getText();
        if (universityNameField.getText() == null || universityNameField.getText().trim().isEmpty()) {
            AlertUtil.showError("Invalid Input", "University Name is a required field.");
            return false;
        }

        return validateDateAndWebLink(deadline, website);
    }

    @FXML private void handleLogout() {
        UserSession.getInstance().logout();
        AlertUtil.showInfo("Logout", "You have been logged out.");
        SceneManager.switchTo("/view/dashboard.fxml");
    }
}
