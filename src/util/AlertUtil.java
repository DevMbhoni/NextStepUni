package util;

import javafx.scene.control.*;
import java.util.Optional;

public class AlertUtil {

    private static final String BUTTON_STYLE = "-fx-background-color: #d51e1e; -fx-text-fill: white; -fx-font-weight: bold;";

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setupAlert(alert, title, message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        setupAlert(alert, title, message);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        setupAlert(alert, title, message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        setupAlert(alert, title, message);
        alert.showAndWait();
    }
    private static void setupAlert(Alert alert, String title, String content) {
        alert.setTitle(title);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(BUTTON_STYLE);
        }

    }
    public static boolean showCustomConfirmation(String title, String header, String content, String confirmText, String cancelText) {
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(cancelButton, confirmButton);

        DialogPane dialogPane = alert.getDialogPane();
        Button confirmBtn = (Button) dialogPane.lookupButton(confirmButton);
        confirmBtn.setStyle("-fx-background-color: #d51e1e; -fx-text-fill: white; -fx-font-weight: bold;");

        Button cancelBtn = (Button) dialogPane.lookupButton(cancelButton);
        cancelBtn.setDefaultButton(true);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }

}
