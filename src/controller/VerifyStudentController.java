package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Student;
import util.AlertUtil;
import util.DatabaseConnector;
import util.EmailService;
import util.SceneManager;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class VerifyStudentController extends BaseController implements Initializable {

    @FXML private Button BackButtonC801;
    @FXML private Button confirmRejectButton;
    @FXML private Button rejectButton;
    @FXML private Button approveButton;
    @FXML private Button cancelRejectButton;
    @FXML private TableColumn<Student, String> EmailCol;
    @FXML private TextField FirstNametxt;
    @FXML private TextField lastNametxt;
    @FXML private TextField Emailtxt;
    @FXML private DatePicker DOBPicker;
    @FXML private TextField unitextfield;
    @FXML private TableView<Student> StudentTable;
    @FXML private TableColumn<Student, String> NameCol;
    @FXML private TableColumn<Student, String> SurnameCol;
    @FXML private ImageView ProofofReg;

    @FXML private VBox ReasonForRejectingVBox;
    @FXML private TextArea rejectionReasonTextArea; 
    @FXML private ProgressIndicator loadingIndicator;


    private final ObservableList<Student> studentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        ReasonForRejectingVBox.setVisible(false);
        loadingIndicator.setVisible(false);

        NameCol.setCellValueFactory(new PropertyValueFactory<>("stuFName"));
        SurnameCol.setCellValueFactory(new PropertyValueFactory<>("stuLName"));
        EmailCol.setCellValueFactory(new PropertyValueFactory<>("emailAddress"));
        NameCol.setSortable(false);
        SurnameCol.setSortable(false);
        EmailCol.setSortable(false);

        loadUnverifiedStudents();
        StudentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) displayStudentDetails(newSel);
        });
    }

    @FXML
    private void onHandleBackButtonC800() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    private void loadUnverifiedStudents() {
        studentList.clear();
        String sql =  """
            SELECT s.StudentID, s.StuFName, s.StuLName, s.EmailAddress, s.StudDob, s.ProofOfReg,
                   (SELECT u.UniName FROM University u WHERE u.UniversityID = s.UniversityID) AS UniName
            FROM Student s
            WHERE s.IsVerified = FALSE AND s.IsRejected = FALSE
            """;

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("StudentID");
                String fName = rs.getString("StuFName");
                String lName = rs.getString("StuLName");
                String email = rs.getString("EmailAddress");
                Date sqlDob = rs.getDate("StudDob");
                LocalDate dob = sqlDob != null ? sqlDob.toLocalDate() : null;
                String proof = rs.getString("ProofOfReg");
                String university = rs.getString("UniName");

                studentList.add(new Student(id, fName, lName, email, dob, proof, university));
            }

            StudentTable.setItems(studentList);

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Failed to load unverified students.");
        }
    }

    private void displayStudentDetails(Student student) {
        FirstNametxt.setText(student.getStuFName());
        lastNametxt.setText(student.getStuLName());
        Emailtxt.setText(student.getEmailAddress());
        DOBPicker.setValue(student.getDateOfBirth());
        DOBPicker.setEditable(false);
        unitextfield.setText(student.getUniversityname());
        loadProofImage(student.getProofOfReg());
    }

    private void loadProofImage(String filename) {
        if (filename != null && !filename.isEmpty()) {
            File file = new File("student_documents", filename);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                ProofofReg.setImage(image);

                ProofofReg.setOnMouseClicked(e -> {
                    Stage stage = new Stage();
                    ImageView imgView = new ImageView(image);
                    imgView.setPreserveRatio(true);
                    imgView.setFitWidth(1000);
                    imgView.setFitHeight(1000);
                    stage.setScene(new Scene(new StackPane(imgView)));
                    stage.setTitle("Proof of Registration");
                    stage.show();
                });
            } else {
                ProofofReg.setImage(null);
            }
        } else {
            ProofofReg.setImage(null);
        }
    }

    @FXML
    private void onHandleApprove() {
        Student selected = StudentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("No Selection", "Please select a student to approve.");
            return;
        }

        if (!AlertUtil.showCustomConfirmation(
                "Confirm Approval",
                "Approve Student",
                "Are you sure you want to approve " + selected.getStuFName() + "?",
                "Approve", "Cancel"
        )) return;

        runAsyncWithLoading(() -> {
            try (Connection conn = DatabaseConnector.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE Student SET IsVerified = TRUE, IsRejected = FALSE WHERE StudentID = ?")) {

                stmt.setInt(1, selected.getStudentID());
                if (stmt.executeUpdate() > 0) {
                    sendApprovalEmail(selected);
                    // Back to FX thread for UI updates
                    javafx.application.Platform.runLater(() -> {
                        AlertUtil.showInfo("Success", selected.getStuFName() + " approved successfully.");
                        clearFields();
                        loadUnverifiedStudents();
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        AlertUtil.showError("Database Error", "Failed to approve student."));
            }
        });
    }


    private void sendApprovalEmail(Student student) {
        EmailService.approveMessage(
                student.getEmailAddress(),
                "Hello " + student.getStuFName() + ",\n\nYour NextStepUni account has been verified successfully!\n\nBest regards,\nStudent Admin Team"
        );
    }

    @FXML
    private void onHandleReject() {
        Student selected = StudentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("No Selection", "Please select a student to reject.");
            return;
        }
        ReasonForRejectingVBox.setVisible(true);
    }

    @FXML
    private void onHandleConfirmReject() {
        Student selected = StudentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String reason = rejectionReasonTextArea.getText().trim();
        if (reason.isEmpty()) {
            AlertUtil.showWarning("Missing Reason", "Please enter a reason for rejection.");
            return;
        }

        if (!AlertUtil.showCustomConfirmation(
                "Confirm Rejection",
                "Reject Student",
                "Are you sure you want to reject " + selected.getStuFName() + "?",
                "Reject", "Cancel"
        )) return;

        runAsyncWithLoading(() -> {
            try (Connection conn = DatabaseConnector.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE Student SET IsRejected = TRUE, IsVerified = FALSE WHERE StudentID = ?")) {

                stmt.setInt(1, selected.getStudentID());
                if (stmt.executeUpdate() > 0) {
                    sendRejectionEmail(selected, reason);
                    javafx.application.Platform.runLater(() -> {
                        AlertUtil.showInfo("Rejected", selected.getStuFName() + " has been rejected.");
                        ReasonForRejectingVBox.setVisible(false);
                        rejectionReasonTextArea.clear();
                        clearFields();
                        loadUnverifiedStudents();
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        AlertUtil.showError("Database Error", "Failed to reject student."));
            }
        });
    }


    @FXML
    private void onHandleConfirmCancel() {
        ReasonForRejectingVBox.setVisible(false);
        rejectionReasonTextArea.clear();
    }

    private void sendRejectionEmail(Student student, String reason) {
        EmailService.rejectMessage("Student Verification Rejected",
                student.getEmailAddress(),
                "Hello " + student.getStuFName() + ",\n\n" +
                        "Unfortunately, your account verification has been rejected.\n\n" +
                        "Reason: " + reason + "\n\n" +
                        "You may try again by uploading a valid Proof of Registration.\n\n" +
                        "Best regards,\nStudent Admin Team"
        );
    }

    private void clearFields() {
        FirstNametxt.clear();
        lastNametxt.clear();
        Emailtxt.clear();
        DOBPicker.setValue(null);
        unitextfield.clear();
        ProofofReg.setImage(null);
    }

    private void runAsyncWithLoading(Runnable task) {
        loadingIndicator.setVisible(true);

        setButtonsDisabled(true);

        Task<Void> backgroundTask = new Task<>() {
            @Override
            protected Void call() {
                task.run();
                return null;
            }
        };

        backgroundTask.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            setButtonsDisabled(false);
        });

        backgroundTask.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            setButtonsDisabled(false);

            Throwable ex = backgroundTask.getException();
            if (ex != null) ex.printStackTrace();
            AlertUtil.showError("Error", "An error occurred during operation.");
        });

        new Thread(backgroundTask).start();
    }


    private void setButtonsDisabled(boolean disable) {
        approveButton.setDisable(disable);
        rejectButton.setDisable(disable);
        confirmRejectButton.setDisable(disable);
        cancelRejectButton.setDisable(disable);
        BackButtonC801.setDisable(disable);
    }

}
