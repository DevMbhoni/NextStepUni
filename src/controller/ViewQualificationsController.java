package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import model.Qualification;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.ResourceBundle;

public class ViewQualificationsController extends BaseController implements Initializable {

    @FXML private TableView<Qualification> qualificationTable;
    @FXML private TableColumn<Qualification, String> nameCol;
    @FXML private TableColumn<Qualification, String> typeCol;
    @FXML private TableColumn<Qualification, String> facultyCol;
    @FXML private TableColumn<Qualification, Void> actionCol;
    @FXML private TextField searchTxt;
    @FXML private ComboBox<String> sortComboBox;

    private final ObservableList<Qualification> qualificationList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHeader();
        configureColumns();
        loadQualifications();
        setupFilteringAndSorting();
        sortComboBox.getItems().addAll("Default", "By Name", "By Type", "By Faculty");
        sortComboBox.setValue("Default");
    }

    private void loadQualifications() {
        qualificationList.clear();
        String query = "SELECT QualID, QualName, QualType, QualFaculty FROM Qualification";
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                qualificationList.add(new Qualification(
                        rs.getInt("QualID"),
                        rs.getString("QualName"),
                        rs.getString("QualType"),
                        rs.getString("QualFaculty")
                ));
            }
            qualificationTable.setItems(qualificationList);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Unable to load qualifications from the database.");
        }
    }

    private void configureColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("qualName"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("qualType"));
        facultyCol.setCellValueFactory(new PropertyValueFactory<>("qualFaculty"));
        actionCol.setCellFactory(createActionCellFactory());

        nameCol.setSortable(false);
        typeCol.setSortable(false);
        facultyCol.setSortable(false);
        actionCol.setSortable(false);
    }

    private void setupFilteringAndSorting() {
        FilteredList<Qualification> filteredList = new FilteredList<>(qualificationList, b -> true);

        searchTxt.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredList.setPredicate(qualification -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return qualification.getQualName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<Qualification> sortedList = new SortedList<>(filteredList);

        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            sortedList.setComparator((q1, q2) -> {
                if ("By Name".equals(newVal)) return q1.getQualName().compareToIgnoreCase(q2.getQualName());
                if ("By Type".equals(newVal)) return q1.getQualType().compareToIgnoreCase(q2.getQualType());
                if ("By Faculty".equals(newVal)) return q1.getQualFaculty().compareToIgnoreCase(q2.getQualFaculty());
                return 0;
            });
        });

        qualificationTable.setItems(sortedList);
    }

    private Callback<TableColumn<Qualification, Void>, TableCell<Qualification, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button editBtn = createIconButton("/images/icons/edit_icon.png");
            private final Button deleteBtn = createIconButton("/images/icons/delete_icon.png");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Qualification qualification = getTableView().getItems().get(getIndex());
                    handleEditQualification(qualification);
                });

                deleteBtn.setOnAction(e -> {
                    Qualification qualification = getTableView().getItems().get(getIndex());
                    handleDeleteQualification(qualification);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private Button createIconButton(String imagePath) {
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
            icon.setFitHeight(18);
            icon.setFitWidth(18);
            Button btn = new Button("", icon);
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 3;");
            return btn;
        } catch (Exception e) {
            Button fallback = new Button("?");
            fallback.setStyle("-fx-background-color: transparent;");
            return fallback;
        }
    }

    private void handleEditQualification(Qualification qualification) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/update_qualification.fxml"));
            Parent root = loader.load();
            UpdateQualificationController controller = loader.getController();
            controller.initData(qualification);
            Stage stage = (Stage) qualificationTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Unable to open edit view.");
        }
    }

    private void handleDeleteQualification(Qualification qualification) {
        boolean confirmed = AlertUtil.showCustomConfirmation(
                "Confirm Delete",
                "Delete Qualification: " + qualification.getQualName(),
                "Are you sure you want to remove this qualification?",
                "Delete",
                "Cancel"
        );

        if (confirmed) {
            try (Connection conn = DatabaseConnector.getInstance().getConnection()) {
                String deleteLinks = "DELETE FROM BursaryQualification WHERE QualID = (SELECT QualID FROM Qualification WHERE QualName=? AND QualType=? AND QualFaculty=?)";
                PreparedStatement stmt1 = conn.prepareStatement(deleteLinks);
                stmt1.setString(1, qualification.getQualName());
                stmt1.setString(2, qualification.getQualType());
                stmt1.setString(3, qualification.getQualFaculty());
                stmt1.executeUpdate();

                String deleteQual = "DELETE FROM Qualification WHERE QualName=? AND QualType=? AND QualFaculty=?";
                PreparedStatement stmt2 = conn.prepareStatement(deleteQual);
                stmt2.setString(1, qualification.getQualName());
                stmt2.setString(2, qualification.getQualType());
                stmt2.setString(3, qualification.getQualFaculty());

                int affectedRows = stmt2.executeUpdate();
                if (affectedRows > 0) {
                    qualificationList.remove(qualification);
                    AlertUtil.showInfo("Deleted", qualification.getQualName() + " removed successfully.");
                } else {
                    AlertUtil.showError("Delete Failed", "Could not find the qualification to delete.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Error", "Failed to delete qualification.");
            }
        }
    }
    @FXML
    private void addQualifClicked(ActionEvent event) {
        SceneManager.switchTo("/view/add_qualification.fxml");
    }

    @FXML
    private void backBtnClicked(ActionEvent event) {
        SceneManager.switchTo("/view/dashboard.fxml");
    }
}
