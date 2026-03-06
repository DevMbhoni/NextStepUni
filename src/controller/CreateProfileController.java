package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Student;
import model.University;
import model.UserSession;

import org.mindrot.jbcrypt.BCrypt;
import util.AlertUtil;
import util.DatabaseConnector;

import util.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;


public class CreateProfileController implements Initializable {

    @FXML private TextField FnameTxt, LnameTxt, emailTxt;
    @FXML private PasswordField PasswordTxt, ConfirmPasswordTxt;
    @FXML private TextField PasswordTxtField, ConfirmPasswordTxtField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private ChoiceBox<University> uniList;
    @FXML private DatePicker DOBDatePicker;
    @FXML private Label regProofNameLbl;
    @FXML private Label warningLbl;
    @FXML private Label otherwarningLbl;
    @FXML private Button importBtn;

    private File proofOfRegFile;
    private Student newStudent;
    private final ObservableList<University> universityList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PasswordTxtField.textProperty().bindBidirectional(PasswordTxt.textProperty());
        ConfirmPasswordTxtField.textProperty().bindBidirectional(ConfirmPasswordTxt.textProperty());

        loadUniversityList();
    }

    private void loadUniversityList() {
        String sql = "SELECT UniversityID, UniName FROM University";
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                universityList.add(new University(
                        rs.getInt("UniversityID"),
                        rs.getString("UniName")
                ));
            }
            uniList.setItems(universityList);

            uniList.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(University university) {
                    return university == null ? "Select University" : university.getUniName();
                }

                @Override
                public University fromString(String string) {
                    return universityList.stream()
                            .filter(uni -> uni.getUniName().equals(string))
                            .findFirst().orElse(null);
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Could not load university list.");
        }
    }

    @FXML
    public void date(ActionEvent event) {
        if (DOBDatePicker.getValue() == null) {
            DOBDatePicker.setBorder(Border.stroke(Color.RED));
        } else {
            DOBDatePicker.setBorder(null);
        }
    }

    @FXML
    public void importBtnAction(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Proof of Registration");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image or PDF Files", "*.jpg", "*.png", "*.jpeg", "*.pdf")
            );
            Stage stage = (Stage) regProofNameLbl.getScene().getWindow();
            proofOfRegFile = fileChooser.showOpenDialog(stage);

            if (proofOfRegFile != null) {
                regProofNameLbl.setText(proofOfRegFile.getName());
                regProofNameLbl.setTextFill(Color.BLACK);
            } else {
                regProofNameLbl.setText("No Proof of Reg found");
            }

        } catch (Exception e) {
            AlertUtil.showError("File Error", "Error importing proof of registration file.");
            e.printStackTrace();
        }
    }

    private boolean emailValidation(String newEmail) {
        return newEmail.matches("^[^@.].*[@].*[.][^@.]+$");
    }

    private boolean notUniqueEmail(String newEmail) {
        String sql = "SELECT 1 FROM Student WHERE EmailAddress=?";
        try (Connection connection = DatabaseConnector.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newEmail);
            ResultSet result = statement.executeQuery();
            return result.next();

        } catch (SQLException e) {
            System.out.println("Error: Cannot check if email is unique\n" + e.getMessage());
        }
        return false;
    }


    @FXML
    public void registerBtnAction(ActionEvent event) {

        String fName = FnameTxt.getText();
        String lName = LnameTxt.getText();
        String email = emailTxt.getText();
        String password = PasswordTxt.getText();
        String confirmPassword = ConfirmPasswordTxt.getText();
        University selectedUniversity = uniList.getValue();
        LocalDate dateOfBirth = DOBDatePicker.getValue();

        warningLbl.setText("");
        otherwarningLbl.setText("");
        FnameTxt.setBorder(null);
        LnameTxt.setBorder(null);
        emailTxt.setBorder(null);
        PasswordTxt.setBorder(null);
        ConfirmPasswordTxt.setBorder(null);
        DOBDatePicker.setBorder(null);
        uniList.setBorder(null);
        regProofNameLbl.setTextFill(Color.BLACK);

        boolean hasError = false;
        if (fName.isBlank()) {
            FnameTxt.setBorder(Border.stroke(Color.RED));
            hasError = true;
        }
        if (lName.isBlank()) {
            LnameTxt.setBorder(Border.stroke(Color.RED));
            hasError = true;
        }
        if (email.isBlank() || !emailValidation(email)) {
            emailTxt.setBorder(Border.stroke(Color.RED));
            if (!hasError) otherwarningLbl.setText("Please enter a valid email.");
            hasError = true;
        }
        if (notUniqueEmail(email)) {
            otherwarningLbl.setText("Email already exists");
            emailTxt.setBorder(Border.stroke(Color.RED));
            hasError = true;
        }
        if (password.length() < 8 || password.isBlank()) {
            otherwarningLbl.setText("Password must be at least 8 characters");
            PasswordTxt.setBorder(Border.stroke(Color.RED));
            hasError = true;
        }
        if (!password.equals(confirmPassword) || confirmPassword.isBlank()) {
            ConfirmPasswordTxt.setBorder(Border.stroke(Color.RED));
            if (!hasError) otherwarningLbl.setText("Passwords do not match");
            hasError = true;
        }
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.of(2007, 12, 31))) {
            DOBDatePicker.setBorder(Border.stroke(Color.RED));
            if (!hasError) otherwarningLbl.setText("Please enter a valid date of birth (must be before 2008).");
            hasError = true;
        }
        if (selectedUniversity == null) {
            if (!hasError) otherwarningLbl.setText("Please select a university.");
            hasError = true;
        }
        if (proofOfRegFile == null) {
            regProofNameLbl.setTextFill(Color.RED);
            if (!hasError) otherwarningLbl.setText("Proof of registration is required.");
            hasError = true;
        }

        if (hasError) {
            warningLbl.setText("Please fix the errors in the form.");
            return;
        }

        try {

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            String savedProofPath = saveProofOfReg(proofOfRegFile, fName + "_" + lName);
            boolean isVerified = false;

            String sql = "INSERT INTO Student (StuLName, StuFName, ProofOfReg, EmailAddress, Password, StudDob, IsVerified, UniversityID) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = DatabaseConnector.getInstance().getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, lName);
                stmt.setString(2, fName);
                stmt.setString(3, savedProofPath);
                stmt.setString(4, email);
                stmt.setString(5, hashedPassword);
                stmt.setDate(6, Date.valueOf(dateOfBirth));
                stmt.setBoolean(7, isVerified);
                stmt.setInt(8, selectedUniversity.getUniversityID());

                stmt.executeUpdate();
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int studentId = generatedKeys.getInt(1);

                    newStudent = new Student(studentId, fName, lName, email, dateOfBirth, savedProofPath, selectedUniversity.getUniName());
                }
            }

            AlertUtil.showInfo("Registration Successful", "Student has been registered successfully. Logging you in...");
            login();
            SceneManager.switchTo("/view/dashboard.fxml");

        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not save student information. Please try again.");
            e.printStackTrace();
        } catch (IOException e) {
            AlertUtil.showError("File Error", "Could not save proof of registration file.");
            e.printStackTrace();
        }
    }


    public void login() {
        if (newStudent != null) {
            UserSession.getInstance().setStudent(newStudent);
            UserSession.getInstance().login(newStudent.getStudentID(), newStudent.getStuFName(), UserSession.UserRole.STUDENT);
        }
    }


    @FXML
    private void handleShowPassword() {
        boolean isSelected = showPasswordCheckBox.isSelected();
        PasswordTxt.setVisible(!isSelected);
        PasswordTxt.setManaged(!isSelected);
        ConfirmPasswordTxt.setVisible(!isSelected);
        ConfirmPasswordTxt.setManaged(!isSelected);

        PasswordTxtField.setVisible(isSelected);
        PasswordTxtField.setManaged(isSelected);
        ConfirmPasswordTxtField.setVisible(isSelected);
        ConfirmPasswordTxtField.setManaged(isSelected);
    }

    @FXML
    public void cancelBtnAction(ActionEvent event) {
        SceneManager.switchTo("/view/login_view.fxml");
    }

    @FXML
    public void loginBtnAction(ActionEvent event) {
        SceneManager.switchTo("/view/login_view.fxml");
    }

    private String saveProofOfReg(File sourceFile, String studentName) throws IOException {
        Path docDir = Paths.get("student_documents");
        if (!Files.exists(docDir)) {
            Files.createDirectories(docDir);
        }

        String sanitizedName = studentName.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
        String fileExtension = getFileExtension(sourceFile.getName());
        String newFileName = sanitizedName + "_proof_" + System.currentTimeMillis() + "." + fileExtension;

        Path destinationFile = docDir.resolve(newFileName);
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
}

