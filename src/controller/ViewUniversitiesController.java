package controller;

import javafx.animation.FadeTransition; // Import FadeTransition
import javafx.animation.KeyFrame; // Import KeyFrame
import javafx.animation.Timeline; // Import Timeline
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import model.University;
import model.UserSession;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

public class ViewUniversitiesController extends BaseController implements Initializable {

    @FXML private FlowPane universityFlowPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Label titleLabel;
    private final List<University> allUniversities = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        animateTitleText(titleLabel);
        loadUniversitiesFromDatabase();
        sortComboBox.getItems().addAll("Default", "By Name (A-Z)", "By Name (Z-A)");
        sortComboBox.setValue("Default");


        searchField.textProperty().addListener((_, _, newValue) -> filterUniversities(newValue));
        sortComboBox.valueProperty().addListener((_, _, _) -> filterUniversities(searchField.getText()));


        filterUniversities(searchField.getText());
    }

    private void animateTitleText(Label label) {
        if (label == null || label.getText() == null) return;
        final String fullText = label.getText();
        label.setText("");

        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().clear();

        for (int i = 0; i < fullText.length(); i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(i * 50),
                    event -> label.setText(label.getText() + fullText.charAt(index))
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }


    private void loadUniversitiesFromDatabase() {
        allUniversities.clear();
        String sql = "SELECT UniversityID, UniName, Location, ApplicationDeadline, Description, WebsiteLink, UniPicturePath FROM University";

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Date sqlDate = rs.getDate("ApplicationDeadline");
                LocalDate deadline = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                allUniversities.add(new University(
                        rs.getInt("UniversityID"),
                        rs.getString("UniName"),
                        rs.getString("Location"),
                        deadline,
                        rs.getString("Description"),
                        rs.getString("WebsiteLink"),
                        rs.getString("UniPicturePath")
                ));
            }
        } catch (Exception e) {
            System.err.println("Error loading universities from the database.");
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Could not load university data.");
        }
    }

    private void displayUniversities(List<University> universitiesToShow) {
        universityFlowPane.getChildren().clear();
        int delay = 0;
        for (University uni : universitiesToShow) {
            Node universityCard = createUniversityCard(uni);
            universityCard.setOpacity(0.0);
            universityFlowPane.getChildren().add(universityCard);

            FadeTransition ft = new FadeTransition(Duration.millis(400), universityCard);
            ft.setToValue(1.0);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            delay += 80;
        }
    }

    private void filterUniversities(String searchText) {
        List<University> filteredList = new ArrayList<>();
        String lowerCaseFilter = (searchText == null) ? "" : searchText.toLowerCase().trim();

        if (lowerCaseFilter.isEmpty()) {
            filteredList.addAll(allUniversities);
        } else {
            for (University uni : allUniversities) {
                if (uni.getUniName() != null && uni.getUniName().toLowerCase().contains(lowerCaseFilter)) {
                    filteredList.add(uni);
                }
            }
        }

        applySorting(filteredList);
        displayUniversities(filteredList);
    }

    private void applySorting(List<University> list) {
        String sortOption = sortComboBox.getValue();
        if ("By Name (A-Z)".equals(sortOption)) {

            list.sort(Comparator.comparing(University::getUniName, Comparator.nullsLast(String::compareToIgnoreCase)));
        } else if ("By Name (Z-A)".equals(sortOption)) {

            list.sort(Comparator.comparing(University::getUniName, Comparator.nullsLast(String::compareToIgnoreCase)).reversed());
        }

    }

    private Node createUniversityCard(University uni) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(220, 250);
        card.setStyle("-fx-background-color: white; -fx-border-color: #d51e1e; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"); // Added shadow effect
        card.setPadding(new javafx.geometry.Insets(10));

        ImageView logoView = new ImageView();
        logoView.setFitHeight(120);
        logoView.setFitWidth(180);
        logoView.setPreserveRatio(true);

        try {
            String imagePath = uni.getUniPicturePath();

            if (imagePath != null && !imagePath.trim().isEmpty()) {
                File imageFile = new File("university_images", imagePath);
                if (imageFile.exists() && imageFile.isFile()) {
                    logoView.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    System.err.println("Image file not found: " + imageFile.getAbsolutePath());
                    loadPlaceholderImage(logoView);
                }
            } else {
                loadPlaceholderImage(logoView);
            }
        } catch (Exception e) {
            System.err.println("Error loading image for university ID " + uni.getUniversityID() + ": " + e.getMessage());
            loadPlaceholderImage(logoView);
        }



        Label nameLabel = new Label(uni.getUniName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nameLabel.setMaxWidth(180);
        Button profileButton = new Button("View Profile");
        profileButton.setFont(Font.font(14));
        profileButton.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: white; -fx-font-weight: bold;");
        profileButton.setCursor(javafx.scene.Cursor.HAND);
        profileButton.setOnAction(event -> {

            SceneManager.switchTo("/view/view_university_profile.fxml",
                    (ViewUniProfileController controller) -> controller.initData(uni)
            );
        });

        card.getChildren().addAll(logoView, nameLabel, profileButton);

        final DropShadow hoverShadow = new DropShadow(20, Color.rgb(213, 30, 30, 0.5)); // Reddish shadow
        card.setOnMouseEntered(e -> card.setEffect(hoverShadow));
        card.setOnMouseExited(e -> card.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.1)))); // Return to subtle shadow



        return card;
    }


    private void loadPlaceholderImage(ImageView imageView) {
        try {
            imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icons/placeholder.png"))));
        } catch(Exception e) {
            System.err.println("CRITICAL: Placeholder image not found!");
        }
    }


    @FXML
    private void handleLoginRegister() {
        SceneManager.switchTo("/view/login_view.fxml");
    }

    @FXML
    private void handleBackClick() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML private void handleHome() { SceneManager.switchTo("/view/dashboard.fxml"); }
    @FXML private void handleViewProfile() { SceneManager.switchTo("/view/view_profile.fxml"); }
    @FXML private void handleLogout() {
        UserSession.getInstance().logout();
        AlertUtil.showInfo("Logout", "You have been logged out.");
        SceneManager.switchTo("/view/dashboard.fxml");
    }

}