package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.util.Duration;
import model.*;
import util.SceneManager;

import java.net.URL;
import java.util.*;


import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class DashboardController extends BaseController implements Initializable {
    @FXML
    private Label welcomeLabel;

    @FXML
    private FlowPane dashboardFlowPane;


    @FXML
    private VBox aboutSection;


    @FXML
    private Text aboutText;


    private final List<University> allUniversities = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        setupDashboard();
        welcomeLabel.setTextFill(Color.BLACK);
        aboutText.setFill(Color.BLACK);
    }


    private void setupDashboard() {
        UserSession session = UserSession.getInstance();
        UserSession.UserRole role = session.getRole();

        boolean isLoggedIn = role != UserSession.UserRole.GUEST;
        welcomeLabel.setVisible(isLoggedIn);
        welcomeLabel.setManaged(isLoggedIn);

        if (isLoggedIn) {
            welcomeLabel.setText("Welcome, " + session.getUserName());
            animateWelcomeText(welcomeLabel);
        }

        boolean isGuest = (role == UserSession.UserRole.GUEST);
        aboutSection.setVisible(isGuest);
        aboutSection.setManaged(isGuest);
        if (isGuest) {
            animateAboutText(aboutText);
        }

        populateDashboardCards(role);
    }

    private void animateWelcomeText(Label label) {
        final String fullText = label.getText();
        label.setText("");

        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().clear();

        for (int i = 0; i < fullText.length(); i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(i * 100),
                    event -> label.setText(label.getText() + fullText.charAt(index))
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }

    private void animateAboutText(Text textNode) {
        final String fullText = textNode.getText();
        textNode.setText("");

        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().clear();

        for (int i = 0; i < fullText.length(); i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(i * 20),
                    event -> textNode.setText(textNode.getText() + fullText.charAt(index))
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }


    private void populateDashboardCards(UserSession.UserRole role) {
        dashboardFlowPane.getChildren().clear();

        List<Node> cardsToAdd = new ArrayList<>();

        if (role == UserSession.UserRole.GUEST) {
            cardsToAdd.add(createDashboardCard("View Universities", "/images/icons/universities_icon.png", this::handleViewUniversities));
            cardsToAdd.add(createDashboardCard("View Bursaries", "/images/icons/bursaries_icon.png", this::handleViewBursaries));
            cardsToAdd.add(createDashboardCard("View Reviews", "/images/icons/reviews_icon.png", this::handleViewReviews));
        } else if (role == UserSession.UserRole.STUDENT) {
            cardsToAdd.add(createDashboardCard("View Universities", "/images/icons/universities_icon.png", this::handleViewUniversities));
            cardsToAdd.add(createDashboardCard("View Bursaries", "/images/icons/bursaries_icon.png", this::handleViewBursaries));
            cardsToAdd.add(createDashboardCard("View Reviews", "/images/icons/reviews_icon.png", this::handleViewReviews));
            cardsToAdd.add(createDashboardCard("Write Reviews", "/images/icons/write_review_icon.png", this::handleWriteReview));
        } else if (role == UserSession.UserRole.ADMIN) {
            cardsToAdd.add(createDashboardCard("Verify Students", "/images/icons/verify_icon.png", this::handleVerify));
            cardsToAdd.add(createDashboardCard("Manage Universities", "/images/icons/universities_icon.png", this::handleManageUniversities));
            cardsToAdd.add(createDashboardCard("Manage Bursaries", "/images/icons/bursaries_icon.png", this::handleMangeBursaries));
            cardsToAdd.add(createDashboardCard("View Qualification", "/images/icons/qualification_icon.png", this::handleViewQualifications));
            cardsToAdd.add(createDashboardCard("Moderate Reviews","/images/icons/write_review_icon.png", this::handleModerate));
        }

        int delay = 0;
        for (Node card : cardsToAdd) {
            card.setOpacity(0.0);
            dashboardFlowPane.getChildren().add(card);

            FadeTransition ft = new FadeTransition(Duration.millis(500), card);
            ft.setToValue(1.0);
            ft.setDelay(Duration.millis(delay));
            ft.play();

            delay += 120;
        }
    }

    @FXML
    private void handleManageUniversities() {
        SceneManager.switchTo("/view/manage_universities.fxml");
    }

    private void handleMangeBursaries() {
        SceneManager.switchTo("/view/manage_bursaries.fxml");
    }

    private void handleModerate() {SceneManager.switchTo("/view/admin_review_moderation.fxml");}
    private void handleViewQualifications() {SceneManager.switchTo("/view/view_qualifications.fxml");}

    @FXML
    private void handleVerify() {
        SceneManager.switchTo("/view/verify_student.fxml");
    }

    private Node createDashboardCard(String title, String imagePath, Runnable action) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setCursor(Cursor.HAND);

        VBox imageContainer = new VBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPrefSize(200, 150);
        imageContainer.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        ImageView icon = new ImageView();
        try {
            icon.setImage(new Image(getClass().getResourceAsStream(imagePath)));
        } catch (Exception e) {
            System.err.println("Could not load icon: " + imagePath);
        }
        icon.setFitHeight(100);
        icon.setFitWidth(100);
        icon.setPreserveRatio(true);
        imageContainer.getChildren().add(icon);

        Button cardButton = new Button(title);
        cardButton.setPrefWidth(200);
        cardButton.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: white; -fx-background-radius: 5;");
        cardButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        cardButton.setOnAction(event -> action.run());

        card.getChildren().addAll(imageContainer, cardButton);
        card.setOnMouseClicked(event -> action.run());

        final DropShadow shadow = new DropShadow(15, Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(15);

        final ScaleTransition stIn = new ScaleTransition(Duration.millis(200), card);
        stIn.setToX(1.03);
        stIn.setToY(1.03);

        final ScaleTransition stOut = new ScaleTransition(Duration.millis(200), card);
        stOut.setToX(1.0);
        stOut.setToY(1.0);

        card.setOnMouseEntered(e -> {
            card.setEffect(shadow);
            stIn.playFromStart();
        });

        card.setOnMouseExited(e -> {
            card.setEffect(null);
            stOut.playFromStart();
        });

        return card;
    }

    @FXML
    private void handleLoginRegister() {
        SceneManager.switchTo("/view/login_view.fxml");
    }

    private void handleViewUniversities() {
        SceneManager.switchTo("/view/view_universities.fxml");
    }

    private void handleViewBursaries() {
        SceneManager.switchTo("/view/view_bursaries.fxml");
    }

    private void handleViewReviews() {
        SceneManager.switchTo("/view/view_review_selection.fxml");
    }

    private void handleWriteReview() {
        SceneManager.switchTo("/view/my_reviews.fxml");
    }

    @FXML
    private void handleViewProfile() {
        SceneManager.switchTo("/view/view_review_selection.fxml");
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        setupDashboard();
    }

    private boolean checkUserLoginStatus() {
        return UserSession.getInstance().isLoggedIn();
    }


    @FXML
    private void homeBtnClicked(javafx.event.ActionEvent event) {

    }
}

