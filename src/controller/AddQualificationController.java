package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Scanner;

public class AddQualificationController extends BaseController implements Initializable {

    @FXML
    private ChoiceBox<String> qualificationTypes;
    @FXML
    private ChoiceBox<String> qualificationFaculties;

    @FXML
    private TextField nameTxt;
    @FXML
    private Label errorLbl;
    @FXML
    private Label nameErrLbl;
    @FXML
    private Label facultyErrLbl;
    @FXML
    private Label typeErrLbl;


    private String qualifName, qualifFaculty, qualifType;

    private final String[] qualifTypes = {"Undergraduate", "Postgraduate"};
    private final ArrayList<String> faculties = new ArrayList<>();
    public ObservableList<Qualification> allQualifications = FXCollections.observableArrayList();

    public ArrayList<String> getFaculties() throws FileNotFoundException {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("/datafiles/facultiesList.txt"))) {
            if (scanner == null) {
                throw new FileNotFoundException("Faculty list file not found in resources.");
            }
            while (scanner.hasNextLine()) {
                faculties.add(scanner.nextLine());
            }
        }
        Collections.sort(faculties);
        System.out.println("Faculties ChoiceBox: " + qualificationFaculties);
        return faculties;
    }

    @FXML
    public void handleCancel(Event event) {
        boolean nameIsEdited = nameTxt.getText() != null && !nameTxt.getText().isEmpty();
        boolean typeIsSelected = qualificationTypes.getValue() != null;
        boolean facultyIsSelected = qualificationFaculties.getValue() != null;

        boolean formEdited = nameIsEdited || typeIsSelected || facultyIsSelected;
        if (formEdited) {
            boolean confirm = AlertUtil.showCustomConfirmation(
                    "Confirm Cancellation",
                    "Unsaved Changes",
                    "Discard the new Qualification information and go back?",
                    "Discard", "Keep Editing"
            );
            if (!confirm) {
                return;
            }
        }

        SceneManager.switchTo("/view/view_qualifications.fxml");

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        try {
            qualificationTypes.getItems().addAll(qualifTypes);
            qualificationFaculties.getItems().addAll(getFaculties());

        } catch (Exception e) {
            System.out.println("---Error---" + e.getMessage());
        }
    }

    @FXML
    public void addBtnOnAction(ActionEvent event) {

        qualifName = nameTxt.getText();
        qualifType = qualificationTypes.getValue();
        qualifFaculty = qualificationFaculties.getValue();

        if(qualifName != null && !qualifName.isBlank() && (qualifFaculty != null) && (qualifType != null)){
            if(existingQualification(qualifName, qualifType, qualifFaculty)){
                AlertUtil.showError("Warning", "Qualification already exists");
            }
            else{
                try (Connection conn = DatabaseConnector.getInstance().getConnection()) {

                    Qualification newQualification = new Qualification(qualifName,qualifType, qualifFaculty);
                    allQualifications.add(newQualification);

                    String sqlQuery = "insert into Qualification(QualName,QualType,QualFaculty) values(?,?,?)";
                    PreparedStatement stmt = conn.prepareStatement(sqlQuery);
                    stmt.setString(1, qualifName);
                    stmt.setString(2, qualifType);
                    stmt.setString(3, qualifFaculty);
                    stmt.executeUpdate();

                    stmt.close();
                    showSuccessAlertAndNavigate(event);
                }catch(Exception e){
                    System.out.println("---Error in adding a qualification---\n" + e.getMessage());

                }
            }

        }
        else{
           AlertUtil.showWarning("Validation","Please fill all the fields");

            if(qualifName.isEmpty()){
                nameErrLbl.setText("Please enter qualification name");

            }
            if(qualifType == null){
                typeErrLbl.setText("Please select a qualification type");
            }
            if(qualifFaculty == null){
                facultyErrLbl.setText("Please select a qualification faculty");
            }
        }

        nameTxt.clear();
        qualificationTypes.getSelectionModel().clearSelection();
        qualificationFaculties.getSelectionModel().clearSelection();
    }

    private void showSuccessAlertAndNavigate(ActionEvent event){
        AlertUtil.showInfo("Confirmation","You have successfully added qualification");
        SceneManager.switchTo("/view/view_qualifications.fxml");
    }
    public boolean existingQualification(String name, String type, String faculty){
        try (Connection conn = DatabaseConnector.getInstance().getConnection())  {
            String sqlQuery = "Select * From Qualification where QualName = ? and QualType = ? and Qualfaculty = ?";
            PreparedStatement stmt = conn.prepareStatement(sqlQuery);
            stmt.setString(1,name);
            stmt.setString(2, type);
            stmt.setString(3, faculty);
            ResultSet result = stmt.executeQuery();
            if(result.next()){
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}