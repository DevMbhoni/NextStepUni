package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.University;
import util.AlertUtil;
import util.SceneManager;
import util.ReviewHelper;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class ViewUniProfileController extends BaseController implements Initializable {

    @FXML private Button backButton;
    @FXML private Button descriptionButton;
    @FXML private Button locationButton;
    @FXML private Button reviewsButton;
    @FXML private Button applicationDeadlineButton;
    @FXML private Label uniNameProfileLabel;
    @FXML private Label locationLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label appDeadlineLabel;
    @FXML private Label websiteLink;
    @FXML private ImageView uniLogoImageView;

    @FXML private HBox descriptionHBox;
    @FXML private HBox locationHBox;
    @FXML private HBox uniProfileReviewHBox;
    @FXML private HBox appDeadlineHBox;
    @FXML private VBox reviewsVBox;
    @FXML private ScrollPane reviewsScrollPane;
    @FXML private HBox reviewsHeaderHBox;
    @FXML private TextArea reviewTextArea;
    @FXML private Button reviewBackButton;

    private University university;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        showSection(descriptionButton, descriptionHBox);
    }

    public void initData(University uni) {
        this.university = uni;
        uniNameProfileLabel.setText(uni.getUniName());
        locationLabel.setText(uni.getLocation());
        descriptionLabel.setText(uni.getDescription());

        LocalDate deadline = uni.getApplicationDeadline();
        if (deadline != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.ENGLISH);
            appDeadlineLabel.setText(deadline.format(formatter));
        } else {
            appDeadlineLabel.setText("No deadline set");
        }


        String website = uni.getWebsiteLink();
        if (website != null && !website.trim().isEmpty()) {
            websiteLink.setText(website);
            websiteLink.setStyle("-fx-text-fill: #f00202; -fx-underline: true;");
            websiteLink.setCursor(Cursor.HAND);
            websiteLink.setOnMouseClicked(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(website));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    AlertUtil.showError("Error", "Unable to open website link");
                }
            });
        } else {
            websiteLink.setText("No website available");
            websiteLink.setStyle("-fx-text-fill: gray;");
        }

        try {
            String imagePath = uni.getUniPicturePath();

            if (imagePath != null && !imagePath.trim().isEmpty()) {
                File imageFile = new File("university_images", imagePath);
                if (imageFile.exists()) {
                    uniLogoImageView.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    System.err.println("Image not found: " + imageFile.getAbsolutePath());
                    uniLogoImageView.setImage(new Image(Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/icons/placeholder.png"))));
                }
            } else {
                uniLogoImageView.setImage(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/icons/placeholder.png"))));
            }

        } catch (Exception e) {
            System.err.println("Error loading image for: " + uni.getUniPicturePath());
            e.printStackTrace();
            uniLogoImageView.setImage(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/icons/placeholder.png"))));
        }

        ReviewHelper.loadReviewsForUniversity(
                uni.getUniversityID(),
                reviewsVBox,
                reviewTextArea,
                reviewBackButton,
                reviewsScrollPane,
                reviewsHeaderHBox
        );
    }


    @FXML
    private void handleBackButton() {
        SceneManager.switchTo("/view/view_universities.fxml");
    }


    @FXML
    private void handleDescriptionClick() {
        showSection(descriptionButton, descriptionHBox);
    }

    @FXML
    private void handleLocationClick() {
        showSection(locationButton, locationHBox);
    }

    @FXML
    private void handleReviewsClick() {
        showSection(reviewsButton, uniProfileReviewHBox);
    }

    @FXML
    private void handleApplicationDeadlineClick() {
        showSection(applicationDeadlineButton, appDeadlineHBox);
    }

    private void showSection(Button activeButton, HBox activeBox) {
        descriptionButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000;");
        locationButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000;");
        reviewsButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000;");
        applicationDeadlineButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000;");

        descriptionHBox.setVisible(false);
        locationHBox.setVisible(false);
        uniProfileReviewHBox.setVisible(false);
        appDeadlineHBox.setVisible(false);

        activeButton.setStyle("-fx-background-color: #f00202; -fx-text-fill: white;");
        activeBox.setVisible(true);
    }
    @FXML
    private void handleReviewBack() {

        reviewTextArea.setVisible(false);
        reviewBackButton.setVisible(false);


        uniProfileReviewHBox.setVisible(true);
    }

    @FXML
    private void handleLoginRegister() {
        SceneManager.switchTo("/view/login_view.fxml");
    }
}
