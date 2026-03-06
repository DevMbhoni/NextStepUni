package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class ViewReviewSelectController extends BaseController implements Initializable {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
    }

    @FXML
    public void onHandleViewUni()
    {
        SceneManager.switchTo("/view/university_selection.fxml");

    }

    @FXML
    public void onHandleViewBurs()
    {
        SceneManager.switchTo("/view/select_review_bursary.fxml");
    }

    @FXML
    public void handleBackClick(){
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML
    private void handleLoginRegister(){
        SceneManager.switchTo("/view/login_view.fxml");
    }

}