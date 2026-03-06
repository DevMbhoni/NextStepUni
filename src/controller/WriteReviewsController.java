package controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class WriteReviewsController extends BaseController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private VBox universityCardVBox;
    @FXML private VBox bursaryCardVBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        animateTitleText(titleLabel);
        applyStaggeredFadeIn();
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

    private void applyStaggeredFadeIn() {
        Node[] cards = {universityCardVBox, bursaryCardVBox};
        int delay = 0;

        for (Node card : cards) {
            if (card != null) {
                card.setOpacity(0.0);

                FadeTransition ft = new FadeTransition(Duration.millis(500), card);
                ft.setToValue(1.0);
                ft.setDelay(Duration.millis(delay));
                ft.play();

                delay += 150;
            }
        }
    }


    @FXML
    public void onHandleWriteUniReviewButton() {
        SceneManager.switchTo("/view/write_uni_review.fxml");
    }

    @FXML
    public void onHandleWriteBursaryReviewButton() {
        SceneManager.switchTo("/view/select_bursary.fxml");
    }

    @FXML
    public void handleBackClick(){
        SceneManager.switchTo("/view/my_reviews.fxml");
    }

    @FXML private void handleHome() { SceneManager.switchTo("/view/dashboard.fxml"); }
    @FXML private void handleViewProfile() { SceneManager.switchTo("/view/view_profile.fxml"); }
    @FXML private void handleLogout() {  }
    @FXML private void handleLoginRegister() { }

}
