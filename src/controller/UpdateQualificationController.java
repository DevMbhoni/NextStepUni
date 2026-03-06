package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Qualification;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UpdateQualificationController extends BaseController implements Initializable {
    @FXML
    private ChoiceBox<String> qualificationTypes;
    @FXML
    private ChoiceBox<String> qualificationFaculties;
    @FXML
    private TextField updateNameTxt;
    @FXML
    private Label errorLbl;
    private Qualification curQualification;
    private String updatequalifName, updatequalifFaculty, updatequalifType;
    private final String[] qualifTypes = {"Undergraduate", "Postgraduate"};
    private final ArrayList<String> faculties = new ArrayList<>();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHeader();
        qualificationTypes.getItems().addAll(qualifTypes);
        try {
            qualificationFaculties.getItems().addAll(getFaculties());
        } catch (Exception e) {
            System.out.println("---Error---" + e.getMessage());
        }
    }

    public void initData(Qualification curQualification) {
        this.curQualification = curQualification;
        updateNameTxt.setText(curQualification.getQualName());
        qualificationFaculties.setValue(curQualification.getQualFaculty());
        qualificationTypes.setValue(curQualification.getQualType());
    }

    public ArrayList<String> getFaculties() throws FileNotFoundException {
        Scanner scanner = new Scanner(getClass().getResourceAsStream("/datafiles/facultiesList.txt"));

        while (scanner.hasNextLine()) {
            faculties.add(scanner.nextLine());
        }
        scanner.close();
        Collections.sort(faculties);
        return faculties;
    }

    @FXML
    public void handleCancel(ActionEvent event) throws IOException {
        boolean confirm = AlertUtil.showConfirmation("Confirm Cancellation",
                "Any unsaved changes will be lost. Are you sure?");
        if (confirm) {
            SceneManager.switchTo("/view/view_qualifications.fxml");
        }
        ;
    }

    public boolean existingQualification(String name, String type, String faculty) {
        try (Connection conn = DatabaseConnector.getInstance().getConnection()) {
            String sqlQuery = "Select * From Qualification where QualName = ? and QualType = ? and Qualfaculty = ?";
            PreparedStatement stmt = conn.prepareStatement(sqlQuery);
            stmt.setString(1, name);
            stmt.setString(2, type);
            stmt.setString(3, faculty);
            ResultSet result = stmt.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    @FXML
    public void updateQualificationOnAction(ActionEvent event) {
        updatequalifName = updateNameTxt.getText();
        updatequalifType = qualificationTypes.getValue();
        updatequalifFaculty = qualificationFaculties.getValue();
        if (updatequalifName == null || updatequalifName.trim().isEmpty() ||
                updatequalifType == null || updatequalifFaculty == null) {
            errorLbl.setText("Please fill all the fields");
            return;
        }

        if (existingQualification(updatequalifName, updatequalifType, updatequalifFaculty)) {
            AlertUtil.showWarning("Warning", "Qualification Already Exists");
            return;
        }

        if (noUpdate(updatequalifName, updatequalifType, updatequalifFaculty)) {
            AlertUtil.showInfo("Warning", "No updates to the qualification");
            return;
        }
        try (Connection conn = DatabaseConnector.getInstance().getConnection()) {
            String sqlQuery = "Update Qualification Set QualName = ? , QualType = ? , QualFaculty = ? where QualID = ?";
            PreparedStatement stmt = conn.prepareStatement(sqlQuery);
            stmt.setString(1, updatequalifName);
            stmt.setString(2, updatequalifType);
            stmt.setString(3, updatequalifFaculty);
            stmt.setInt(4, curQualification.getQualID());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                AlertUtil.showInfo("Success", "You have successfully updated qualification");
            }

        } catch (Exception e) {

            System.out.println("------QAULIF NOT ADDED-------");
        }
    }
    public boolean noUpdate(String name, String type, String faculty){
        return Objects.equals(curQualification.getQualName(), name)
                && Objects.equals(curQualification.getQualType(), type)
                && Objects.equals(curQualification.getQualFaculty(), faculty);
    }

}
