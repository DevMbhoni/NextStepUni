import util.DatabaseConnector;
import util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        DatabaseConnector.getInstance().connect();


        SceneManager.setStage(primaryStage);
        primaryStage.setTitle("NextStepUni");
        primaryStage.setMaximized(true);


        SceneManager.switchTo("/view/login_view.fxml");
    }


    @Override
    public void stop() throws Exception {
        DatabaseConnector.getInstance().disconnect();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
