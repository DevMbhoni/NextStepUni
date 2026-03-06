package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import model.Bursary;
import util.DatabaseConnector;
import util.SceneManager;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SelectBursaryWriteController extends BaseController implements Initializable {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private VBox bursaryContainer;

    private final ObservableList<Bursary> masterBursaryList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        loadBursariesFromDatabase();
        setupFilterAndSortListeners();

        sortComboBox.getItems().addAll("Default", "By Name (A-Z)", "By Deadline (Soonest)");
        sortComboBox.setValue("Default");

        updateBursaryDisplay();
    }

    private void loadBursariesFromDatabase() {
        masterBursaryList.clear();
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT BursaryID,BurName, ApplicationDeadline, Description, WebsiteLink FROM Bursary")) {

            while (rs.next()) {
                int bursaryID = rs.getInt("BursaryID");
                String name = rs.getString("BurName");
                LocalDate deadline = rs.getDate("ApplicationDeadline").toLocalDate();
                String description = rs.getString("Description");
                String website = rs.getString("WebsiteLink");
                masterBursaryList.add(new Bursary(bursaryID,name, deadline, description, website));
            }
        } catch (Exception e) {
            System.err.println("Error loading bursaries from the database. "+ e.getMessage());

        }
    }

    private void setupFilterAndSortListeners() {
        searchField.textProperty().addListener((obs, old, val) -> updateBursaryDisplay());
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> updateBursaryDisplay());
    }

    private void updateBursaryDisplay() {
        List<Bursary> filteredList = masterBursaryList.stream()
                .filter(b -> {
                    String searchText = searchField.getText();
                    if (searchText == null || searchText.isEmpty()) {
                        return true;
                    }
                    return b.getBurName().toLowerCase().contains(searchText.toLowerCase());
                })
                .collect(Collectors.toList());

        String sortOption = sortComboBox.getValue();
        if ("By Name (A-Z)".equals(sortOption)) {
            filteredList.sort(Comparator.comparing(Bursary::getBurName));
        } else if ("By Deadline (Soonest)".equals(sortOption)) {
            filteredList.sort(Comparator.comparing(Bursary::getApplicationDeadline));
        }

        populateBursaryRows(filteredList);
    }

    private void populateBursaryRows(List<Bursary> bursaries) {
        bursaryContainer.getChildren().clear();
        for (Bursary bursary : bursaries) {
            bursaryContainer.getChildren().add(createBursaryRow(bursary));
        }
    }

    private HBox createBursaryRow(Bursary bursary) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        Label nameLabel = new Label(bursary.getBurName());
        nameLabel.setPrefWidth(350.0);
        nameLabel.setWrapText(true);

        Label deadlineLabel = new Label(bursary.getApplicationDeadline().toString());
        deadlineLabel.setPrefWidth(200.0);

        Hyperlink websiteLink = new Hyperlink("[Link]");
        websiteLink.setOnAction(e -> handleWebsiteLink(bursary.getWebsiteLink()));
        HBox websiteContainer = new HBox(websiteLink);
        websiteContainer.setPrefWidth(150.0);
        websiteContainer.setAlignment(Pos.CENTER);

        Button detailsButton = new Button("Select");
        detailsButton.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        detailsButton.setOnAction(e -> handleSelect(bursary));
        HBox buttonContainer = new HBox(detailsButton);
        buttonContainer.setPrefWidth(200.0);
        buttonContainer.setAlignment(Pos.CENTER);
        detailsButton.setFont(Font.font(14));

        row.getChildren().addAll(nameLabel, deadlineLabel, websiteContainer, buttonContainer);
        return row;
    }

    private void handleWebsiteLink(String url) {
        try {
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

    private void handleSelect(Bursary bursary) {
        SceneManager.switchTo("/view/write_bursary_review.fxml",
                (WriteBursaryReviewController controller) -> controller.initData(bursary));
    }

    @FXML
    private void handleBackClick(){
        SceneManager.switchTo("/view/write_reviews.fxml");
    }
    @FXML
    private void handleLoginRegister() {
        SceneManager.switchTo("/view/login_view.fxml");
    }
}
