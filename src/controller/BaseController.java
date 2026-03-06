package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import model.UserSession;
import util.SceneManager;

public abstract class BaseController {


    @FXML
    private Button loginButton;

    @FXML
    private MenuButton profileButton;

    protected void setupHeader() {
        UserSession session = UserSession.getInstance();

        switch (session.getRole()) {
            case STUDENT:
                if (profileButton != null) {
                    profileButton.setVisible(true);
                    profileButton.setManaged(true);

                    profileButton.getItems().clear();
                    profileButton.getItems().add(new MenuItem("View Profile", null));
                    profileButton.getItems().add(new MenuItem("Logout", null));

                    profileButton.getItems().get(0).setOnAction(e -> handleViewProfile());
                    profileButton.getItems().get(1).setOnAction(e -> handleLogout());
                }
                if (loginButton != null) {
                    loginButton.setVisible(false);
                    loginButton.setManaged(false);
                }
                break;

            case ADMIN:
                if (profileButton != null) {
                    profileButton.setVisible(true);
                    profileButton.setManaged(true);
                    profileButton.getItems().clear();
                    MenuItem logoutItem = new MenuItem("Logout");
                    logoutItem.setOnAction(e -> handleLogout());
                    profileButton.getItems().add(logoutItem);
                }
                if (loginButton != null) {
                    loginButton.setVisible(false);
                    loginButton.setManaged(false);
                }
                break;

            case GUEST:
            default:
                if (profileButton != null) {
                    profileButton.setVisible(false);
                    profileButton.setManaged(false);
                }
                if (loginButton != null) {
                    loginButton.setVisible(true);
                    loginButton.setManaged(true);
                }
                break;
        }
    }
    @FXML
    private void handleViewProfile() {
        SceneManager.switchTo("/view/view_profile.fxml");
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML
    private void handleLoginRegister(){
        SceneManager.switchTo("/view/login_view.fxml");
    }
    @FXML
    private void homeBtnClicked(){
        SceneManager.switchTo("/view/dashboard.fxml");
    }
}
