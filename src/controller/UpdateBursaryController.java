package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import model.Bursary;
import model.Qualification;
import model.University;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

import static controller.AddUniversityController.validateDateAndWebLink;

public class UpdateBursaryController extends BaseController implements Initializable {
    @FXML
    private TextField bursaryNameField;

    @FXML
    private TextField websiteLinkField;

    @FXML
    private DatePicker applicationDeadlineField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ListView<Qualification> qualificationListView;

    private Bursary currentBursary;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
    }


    public void initData(Bursary bursary) {
        this.currentBursary = bursary;
        bursaryNameField.setText(bursary.getBurName());
        websiteLinkField.setText(bursary.getWebsiteLink());
        descriptionField.setText(bursary.getDescription());
        applicationDeadlineField.setValue(bursary.getApplicationDeadline());
        loadQualifications();
        preselectQualifications(bursary.getBursaryID());
    }

    private void loadQualifications() {
        try (Connection conn = DatabaseConnector.getInstance().getConnection()) {
            String sql = "SELECT QualID, QualName, QualType, QualFaculty FROM Qualification";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            ObservableList<Qualification> qualificationList = FXCollections.observableArrayList();
            while (rs.next()) {
                Qualification q = new Qualification(
                        rs.getInt("QualID"),
                        rs.getString("QualName"),
                        rs.getString("QualType"),
                        rs.getString("QualFaculty")
                );
                qualificationList.add(q);
            }
            qualificationListView.setItems(qualificationList);
            qualificationListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            if (currentBursary != null) {
                preselectQualifications(currentBursary.getBursaryID());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleUpdate(ActionEvent event) {
        if (!validateInput()) {
            return;
        }
        String sql = "UPDATE Bursary SET BurName = ?, ApplicationDeadline = ?, Description = ?, WebsiteLink = ?  WHERE BursaryID = ?";
        Connection conn = DatabaseConnector.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bursaryNameField.getText());
            pstmt.setDate(2, java.sql.Date.valueOf(applicationDeadlineField.getValue()));
            pstmt.setString(3,descriptionField.getText());
            pstmt.setString(4, websiteLinkField.getText());
            pstmt.setInt(5,currentBursary.getBursaryID());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                updateBursaryQualifications(conn, currentBursary.getBursaryID());
                AlertUtil.showInfo("Success", "Bursary updated successfully.");
                SceneManager.switchTo("/view/manage_bursaries.fxml");
            }
            else {
                AlertUtil.showError( "Update Failed", "Could not update the bursary in the database. Please try again.");
            }

        }catch (SQLException e){
            AlertUtil.showError("Unexpected Error","Unable to update the bursary. Please try again.");
        }
    }
    private void updateBursaryQualifications(Connection conn, int bursaryID) throws SQLException {
        String deleteSql = "DELETE FROM BursaryQualification WHERE BursaryID = ?";
        PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
        deleteStmt.setInt(1, bursaryID);
        deleteStmt.executeUpdate();

        ObservableList<Qualification> selectedQuals = qualificationListView.getSelectionModel().getSelectedItems();

        if (selectedQuals == null || selectedQuals.isEmpty()) {
            return;
        }

        String insertSql = "INSERT INTO BursaryQualification (QualID, BursaryID) VALUES (?, ?)";
        PreparedStatement insertStmt = conn.prepareStatement(insertSql);

        for (Qualification q : selectedQuals) {
            insertStmt.setInt(1, q.getQualID());
            insertStmt.setInt(2, bursaryID);
            insertStmt.addBatch();
        }

        insertStmt.executeBatch();
    }

    private void preselectQualifications(int bursaryID) {
        try (Connection conn = DatabaseConnector.getInstance().getConnection()) {
            String sql = "SELECT QualID FROM BursaryQualification WHERE BursaryID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, bursaryID);
            ResultSet rs = pstmt.executeQuery();

            ObservableList<Qualification> items = qualificationListView.getItems();
            while (rs.next()) {
                int qualID = rs.getInt("QualID");
                for (Qualification q : items) {
                    if (q.getQualID() == qualID) {
                        qualificationListView.getSelectionModel().select(q);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void handleCancel(Event event) {
        boolean confirm = AlertUtil.showConfirmation("Confirm Cancellation", "Any unsaved changes will be lost. Are you sure?");
        if (confirm) {
            SceneManager.switchTo("/view/manage_bursaries.fxml");
        }
    }
    private boolean validateInput() {
        String name = bursaryNameField.getText();
        String website = websiteLinkField.getText();
        LocalDate deadline = applicationDeadlineField.getValue();

        if (name == null || name.trim().isEmpty()) {
            AlertUtil.showError("Validation Error", "Bursary name is required.");
            return false;
        }
        if (qualificationListView.getSelectionModel().getSelectedItems().isEmpty()) {
            AlertUtil.showError("Validation Error", "Select at least one qualification.");
            return false;
        }

        return validateDateAndWebLink(deadline, website);
    }



}
