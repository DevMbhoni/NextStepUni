package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import model.Bursary;
import model.Qualification;
import util.DatabaseConnector;
import util.SceneManager;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class BursaryDetailsController extends BaseController implements Initializable {

    @FXML
    private Label bursaryNameLabel;
    @FXML
    private Label deadlineLabel;
    @FXML
    private Hyperlink websiteLink;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ListView<Qualification> qualificationsListView;

    private Bursary currentBursary;
    private final ObservableList<Qualification> qualificationsList = FXCollections.observableArrayList();

    public void initData(Bursary bursary) {
        currentBursary = bursary;
        bursaryNameLabel.setText(currentBursary.getBurName());
        deadlineLabel.setText(currentBursary.getApplicationDeadline().toString());
        websiteLink.setText(currentBursary.getWebsiteLink());
        descriptionArea.setText(currentBursary.getDescription());

        loadQualifications(currentBursary.getBursaryID());
    }


    private void loadQualifications(int bursaryID) {
        qualificationsList.clear();
        String sql = "SELECT q.QualID, q.QualName, q.QualType, q.QualFaculty " +
                "FROM Qualification q " +
                "JOIN BursaryQualification bq ON q.QualID = bq.QualID " +
                "WHERE bq.BursaryID = ?";

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bursaryID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                qualificationsList.add(new Qualification(
                        rs.getInt("QualID"),
                        rs.getString("QualName"),
                        rs.getString("QualType"),
                        rs.getString("QualFaculty")
                ));
            }
            qualificationsListView.setItems(qualificationsList);

        } catch (SQLException e) {
            System.err.println("Error loading qualifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleApplyNow() {
        handleWebsiteClick();
    }

    @FXML
    private void handleWebsiteClick() {
        try {
            String url = currentBursary.getWebsiteLink();
            if (url != null && !url.isEmpty()) {
                String fullUrl = url;
                if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
                    fullUrl = "https://" + url;
                }
                Desktop.getDesktop().browse(new URI(fullUrl));
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Failed to open link: " + e.getMessage());
        }
    }

    @FXML
    private void handleLoginRegister() {
        SceneManager.switchTo("/view/login_view.fxml");
    }
    @FXML
    private void handleBackClick() {
        SceneManager.switchTo("/view/view_bursaries.fxml");
    }

    @FXML
    private void handleGoHome() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
    }
}
