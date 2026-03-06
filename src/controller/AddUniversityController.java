package controller;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

public class AddUniversityController extends BaseController implements Initializable {

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
    @FXML
    private VBox mainContentVBox;
    @FXML
    private ProgressIndicator loadingIndicator;


    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
    }


    @FXML
    private void handleAddImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select University Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg","*.webp")
        );
        Stage stage = (Stage) universityImageView.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            try {
                Image image = new Image(selectedImageFile.toURI().toString());
                universityImageView.setImage(image);
            } catch (Exception e) {
                AlertUtil.showError("Image Error", "Could not load the selected image.");
                e.printStackTrace();
            }
        }
    }


    @FXML
    private void handleRemoveImage() {
        universityImageView.setImage(null);
        selectedImageFile = null;
    }

    public void handleSave(Event event) {
        if (!validateInput()) {
            return;
        }
        showLoading(true);

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                String sql = "INSERT INTO University(UniName, Location, WebsiteLink, ApplicationDeadline, Description, UniPicturePath) VALUES(?, ?, ?, ?, ?, ?)";
                try (Connection conn = DatabaseConnector.getInstance().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, universityNameField.getText());
                    pstmt.setString(2, locationField.getText());
                    pstmt.setString(3, websiteLinkField.getText());
                    pstmt.setDate(4, java.sql.Date.valueOf(applicationDeadlinePicker.getValue()));
                    pstmt.setString(5, descriptionArea.getText());

                    if (selectedImageFile != null) {
                        String relativePath = saveImageToDataDirectory(selectedImageFile, universityNameField.getText());
                        pstmt.setString(6, relativePath);
                    } else {
                        pstmt.setNull(6, java.sql.Types.VARCHAR);
                    }

                    int rows = pstmt.executeUpdate();
                    return rows > 0;
                }
            }
        };


        saveTask.setOnSucceeded(e -> {
            showLoading(false);
            if (saveTask.getValue()) {
                showSuccessAlertAndNavigate(event);
            } else {
                AlertUtil.showError("Save Failed", "The university could not be saved to the database.");
            }
        });

        saveTask.setOnFailed(e -> {
            showLoading(false);
            Throwable exception = saveTask.getException();
            if (exception instanceof IOException) {
               AlertUtil.showError("File Error", "Could not save the image file.");
               System.out.println(exception.getMessage());
            } else if (exception instanceof SQLException) {
                AlertUtil.showError("Unexpected Error", "Something went wrong while accessing the information. Please try again.");
                System.out.println(exception.getMessage());
            } else {
                AlertUtil.showError("Unexpected Error", "An unexpected error occurred during the save operation. Please try again.");
            }
            exception.printStackTrace();
        });

        new Thread(saveTask).start();
    }

    @FXML
    public void handleCancel(Event event) {
        boolean formEdited = !universityNameField.getText().isEmpty()
                || !locationField.getText().isEmpty()
                || !websiteLinkField.getText().isEmpty()
                || applicationDeadlinePicker.getValue() != null
                || !descriptionArea.getText().isEmpty()
                || selectedImageFile != null;

        if (formEdited) {
            boolean confirm = AlertUtil.showCustomConfirmation(
                    "Confirm Cancellation",
                    "Unsaved Changes",
                    "Discard the new university information and go back?",
                    "Discard", "Keep Editing"
            );
            if (!confirm) {
                return;
            }
        }
        SceneManager.switchTo("/view/manage_universities.fxml");
    }


    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        mainContentVBox.setDisable(isLoading);
    }


    private boolean validateInput() {
        LocalDate deadline = applicationDeadlinePicker.getValue();
        String website = websiteLinkField.getText();
        String uniName = universityNameField.getText();
        if (uniName == null || uniName.trim().isEmpty()) {
            AlertUtil.showError("Invalid Input", "University Name is a required field.");
            return false;
        }

        if (universityExists(uniName.trim())) {
            AlertUtil.showError("Duplicate Entry", "A university with the name '" + uniName.trim() + "' already exists.");
            return false;
        }

        return validateDateAndWebLink(deadline, website);
    }

    static boolean validateDateAndWebLink(LocalDate deadline, String website) {
        if (website != null && !website.trim().isEmpty()) {
            if (!website.matches("^(http|https)://.*$")) {
                AlertUtil.showError("Validation Error", "Website link must start with http:// or https://");
                return false;
            }
        }
        if (deadline == null) {
            AlertUtil.showError("Validation Error", "Application deadline is required.");
            return false;
        }
        if( deadline.isBefore(LocalDate.now())) {
            AlertUtil.showError("Validation Error", "Application deadline cannot be in the past.");
            return false;
        }

        return true;
    }


    private void showSuccessAlertAndNavigate(Event event) {
        AlertUtil.showInfo("Success", "University Added Successfully");
        SceneManager.switchTo("/view/manage_universities.fxml");
    }

    private String saveImageToDataDirectory(File sourceFile, String universityName) throws IOException {
        Path imageDir = Paths.get("university_images");
        if (!Files.exists(imageDir)) {
            Files.createDirectories(imageDir);
            System.out.println("Created image directory: " + imageDir.toAbsolutePath());
        }

        String sanitizedName = universityName.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
        String fileExtension = getFileExtension(sourceFile.getName());
        String newFileName = sanitizedName + "_" + System.currentTimeMillis() + "." + fileExtension;

        Path destinationFile = imageDir.resolve(newFileName);
        System.out.println("Copying image to: " + destinationFile.toAbsolutePath());
        Files.copy(sourceFile.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        return newFileName;
    }
    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
    private boolean universityExists(String name) {
        String sql = "SELECT COUNT(*) FROM University WHERE LCase(UniName) = LCase(?)";
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Could not check if university exists. Please try again.");
            return true;
        } catch (NullPointerException e) {
            AlertUtil.showError("Connection Error", "Failed to get database connection for checking duplicates.");
            e.printStackTrace();
            return true;
        }
        return false;
    }

}
