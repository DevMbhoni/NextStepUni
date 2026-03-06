package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import model.UserSession;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ViewProfileController extends BaseController implements Initializable {

    @FXML private Label studentInitialsLbl;
    @FXML private Label studentEmailLbl;
    @FXML private Label StudentNameLbl;


    @FXML private TextField viewFnameTxt;
    @FXML private TextField viewLnameTxt;
    @FXML private TextField viewemailTxt;
    @FXML private TextField viewDateOfBirthTxt;
    @FXML private TextField viewUniversityNameTxt;

    private int studentId;
    private String proofOfReg;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHeader();
        studentId = UserSession.getInstance().getId();

        if (studentId != 0) {
            loadStudentProfile();
        } else {
            AlertUtil.showError("Error", "No active student session found.");
            SceneManager.switchTo("/view/login_view.fxml");
        }
    }

    private void loadStudentProfile() {
        String query = "SELECT s.StuFName, s.ProofOfReg,s.StuLName, s.EmailAddress, s.StudDob, u.UniName " +
                "FROM Student s " +
                "LEFT JOIN University u ON s.UniversityID = u.UniversityID " +
                "WHERE s.StudentID = ?";

        try (Connection connection = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String fName = rs.getString("StuFName");
                String lName = rs.getString("StuLName");
                String email = rs.getString("EmailAddress");
                LocalDate dob = rs.getDate("StudDob").toLocalDate();
                String uniName = rs.getString("UniName");
                proofOfReg = rs.getString("ProofOfReg");

                StudentNameLbl.setText(fName + " " + lName);
                studentEmailLbl.setText(email);
                if (fName != null && !fName.isEmpty() && lName != null && !lName.isEmpty()) {
                    studentInitialsLbl.setText((fName.substring(0, 1) + lName.substring(0, 1)).toUpperCase());
                }


                viewFnameTxt.setText(fName);
                viewLnameTxt.setText(lName);
                viewemailTxt.setText(email);
                viewDateOfBirthTxt.setText(dob.toString());
                viewUniversityNameTxt.setText(uniName != null ? uniName : "N/A");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Could not load profile.");
        }
    }

    @FXML
    private void updateProfileBtnAction(ActionEvent event) {
        SceneManager.switchTo("/view/update_profile.fxml");
    }

    @FXML
    private void cancelBtnClicked(ActionEvent event) {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML
    private void deleteBtnClicked(ActionEvent event) throws IOException {
        boolean confirmed = AlertUtil.showCustomConfirmation("Delete Profile",
                "Are you sure you want to delete your profile?",
                "This action is permanent and cannot be undone. All your reviews will also be deleted.",
                "Delete My Account", "Cancel");

        if (confirmed) {
            String deleteUniReviewsQuery = "DELETE FROM UniversityReview WHERE StudentID = ?";
            String deleteBursaryReviewsQuery = "DELETE FROM BursaryReview WHERE StudentID = ?";
            String deleteStudentQuery = "DELETE FROM Student WHERE StudentID = ?";

            Connection connection = null;
            boolean success = false;

            try {
                connection = DatabaseConnector.getInstance().getConnection();
                connection.setAutoCommit(false);

                try (PreparedStatement stmtUni = connection.prepareStatement(deleteUniReviewsQuery)) {
                    stmtUni.setInt(1, studentId);
                    stmtUni.executeUpdate();
                }


                try (PreparedStatement stmtBurs = connection.prepareStatement(deleteBursaryReviewsQuery)) {
                    stmtBurs.setInt(1, studentId);
                    stmtBurs.executeUpdate();
                }


                try (PreparedStatement stmtStu = connection.prepareStatement(deleteStudentQuery)) {
                    stmtStu.setInt(1, studentId);
                    int rows = stmtStu.executeUpdate();
                    if (rows > 0) {
                        success = true;
                        Path imagePath = Paths.get("student_documents").resolve(proofOfReg);
                        Files.deleteIfExists(imagePath);
                    } else {
                        AlertUtil.showError("Delete Failed", "Could not find your student account to delete (it might have already been deleted).");
                    }
                }

                connection.commit();

            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtil.showError("Unexpecetd Error", "Failed to delete profile due to a database error.");
                if (connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                success = false;

            } finally {
                if (connection != null) {
                    try {
                        connection.setAutoCommit(true);

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (success) {
                UserSession.getInstance().logout();
                AlertUtil.showInfo("Profile Deleted", "Your account and all associated data have been successfully deleted.");
                SceneManager.switchTo("/view/dashboard.fxml");
            }

        }
    }


    @FXML
    private void handleHome() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML
    private void handleViewProfile() {
        loadStudentProfile();
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        SceneManager.switchTo("/view/dashboard.fxml");
    }
}
