package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import model.University;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;


import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManageUniversitiesController extends BaseController implements Initializable {

    @FXML
    private TableView<University> universityTableView;
    @FXML
    private TableColumn<University, String> nameColumn;
    @FXML
    private TableColumn<University, String> locationColumn;
    @FXML
    private TableColumn<University, LocalDate> deadlineColumn;
    @FXML
    private TableColumn<University, String> websiteColumn;
    @FXML
    private TableColumn<University, Void> actionColumn;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;


    private final ObservableList<University> universityList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        loadUniversitiesFromDatabase();
        configureTableColumns();
        setupFilterAndSort();

        sortComboBox.getItems().addAll("Default", "By Name", "By Location", "By Deadline");
        sortComboBox.setValue("Default");
    }
    private void loadUniversitiesFromDatabase() {
        universityList.clear();

        String sql = "SELECT UniversityID, UniName, Location, ApplicationDeadline, Description, WebsiteLink, UniPicturePath FROM University";

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                universityList.add(new University(
                        rs.getInt("UniversityID"),
                        rs.getString("UniName"),
                        rs.getString("Location"),
                        rs.getDate("ApplicationDeadline").toLocalDate(),
                        rs.getString("Description"),
                        rs.getString("WebsiteLink"),
                        rs.getString("UniPicturePath")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("uniName"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        deadlineColumn.setCellValueFactory(new PropertyValueFactory<>("applicationDeadline"));
        nameColumn.setSortable(false);
        locationColumn.setSortable(false);
        deadlineColumn.setSortable(false);
        websiteColumn.setSortable(false);
        actionColumn.setSortable(false);

        websiteColumn.setCellFactory(createWebsiteCellFactory());
        actionColumn.setCellFactory(createActionCellFactory());
    }

    private void setupFilterAndSort() {
        FilteredList<University> filteredData = new FilteredList<>(universityList, b -> true);

        searchField.textProperty().addListener((_, _, aNew) -> {
            filteredData.setPredicate(university -> {
                if (aNew == null || aNew.isEmpty()) return true;
                return university.getUniName().toLowerCase().contains(aNew.toLowerCase());
            });
        });

        SortedList<University> sortedData = new SortedList<>(filteredData);


        sortComboBox.valueProperty().addListener((_, _, newVal) -> {
            sortedData.setComparator((u1, u2) -> {
                if ("By Name".equals(newVal)) {
                    return u1.getUniName().compareToIgnoreCase(u2.getUniName());
                } else if ("By Location".equals(newVal)) {
                    return u1.getLocation().compareToIgnoreCase(u2.getLocation());
                } else if ("By Deadline".equals(newVal)) {
                    return u1.getApplicationDeadline().compareTo(u2.getApplicationDeadline());
                } else {
                    return 0;
                }
            });
        });

        universityTableView.setItems(sortedData);
    }


    private Callback<TableColumn<University, String>, TableCell<University, String>> createWebsiteCellFactory() {
        return param -> new TableCell<>() {
            private final Hyperlink link = new Hyperlink("[Link]");
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    University uni = getTableView().getItems().get(getIndex());
                    link.setOnAction(event -> handleWebsiteLink(uni.getWebsiteLink()));
                    setGraphic(link);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private Callback<TableColumn<University, Void>, TableCell<University, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button editButton = createIconButton("/images/icons/edit_icon.png");
            private final Button deleteButton = createIconButton("/images/icons/delete_icon.png");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                pane.setAlignment(Pos.CENTER);
                editButton.setOnAction(event -> {
                    University university = getTableView().getItems().get(getIndex());
                    handleEditUniversity(university);
                });
                deleteButton.setOnAction(event -> {
                    University university = getTableView().getItems().get(getIndex());
                    handleDeleteUniversity(university);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        };
    }

    @FXML
    private void handleAddUniversity() {
        SceneManager.switchTo("/view/add_university.fxml");
    }

    private void handleEditUniversity(University university) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/update_university.fxml"));
            Parent root = loader.load();

            UpdateUniversityController controller = loader.getController();

            controller.initData(university);


            Stage stage = (Stage) universityTableView.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Updated handleDeleteUniversity ---
    private void handleDeleteUniversity(University university) {
        boolean confirmed = AlertUtil.showCustomConfirmation(
                "Confirm Deletion",
                "Delete University: " + university.getUniName(),
                "Are you sure? This will also remove associated student reviews AND potentially student links, and cannot be undone.", // Updated message
                "Delete",
                "Cancel"
        );

        if (confirmed) {
            Connection conn = null; // Declare connection outside try to manage transaction
            boolean success = false;
            try {
                conn = DatabaseConnector.getInstance().getConnection();
                conn.setAutoCommit(false);

                String deleteReviewsSql = "DELETE FROM UniversityReview WHERE UniversityID = ?";
                try (PreparedStatement stmtReviews = conn.prepareStatement(deleteReviewsSql)) {
                    stmtReviews.setInt(1, university.getUniversityID());
                    stmtReviews.executeUpdate();
                }

                String updateStudentsSql = "UPDATE Student SET UniversityID = NULL WHERE UniversityID = ?";
                try (PreparedStatement stmtUpdate = conn.prepareStatement(updateStudentsSql)) {
                    stmtUpdate.setInt(1, university.getUniversityID());
                    stmtUpdate.executeUpdate();
                }

                String deleteUniversitySql = "DELETE FROM University WHERE UniversityID = ?";
                try (PreparedStatement stmtUniversity = conn.prepareStatement(deleteUniversitySql)) {
                    stmtUniversity.setInt(1, university.getUniversityID());
                    int affectedRows = stmtUniversity.executeUpdate();
                    if (affectedRows > 0) {
                        success = true;
                    } else {
                        AlertUtil.showError("Delete Failed", "Could not find the university (ID: " + university.getUniversityID() + ") to delete.");
                    }
                }

                conn.commit();

            } catch (SQLException e) {
                success = false;
                System.err.println("Database error during university deletion: " + e.getMessage());
                e.printStackTrace();
                if (e.getMessage().contains("foreign key constraint")) {
                    AlertUtil.showError("Deletion Blocked", "Cannot delete university: Other records (like Students) still reference it.");
                } else {
                    AlertUtil.showError("Database Error", "Failed to delete university due to a database error.");
                }
                try {
                    if (conn != null) conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            } catch (NullPointerException e) {
                success = false;
                System.err.println("Error getting database connection.");
                e.printStackTrace();
                AlertUtil.showError("Connection Error", "Could not connect to the database.");
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    System.err.println("Error restoring auto-commit: " + e.getMessage());
                }
            }

            if (success) {

                universityList.remove(university);
                AlertUtil.showInfo("Deleted", university.getUniName() + " was successfully removed.");

                String imageFileName = university.getUniPicturePath();
                if (imageFileName != null && !imageFileName.trim().isEmpty()) {
                    try {
                        Path imagePath = Paths.get("university_images").resolve(imageFileName);
                        boolean deleted = Files.deleteIfExists(imagePath);
                        if (deleted) {
                            System.out.println("Deleted image file: " + imagePath.toAbsolutePath());
                        } else {
                            System.out.println("Image file not found for deletion: " + imagePath.toAbsolutePath());
                        }
                    } catch (IOException e) {
                        System.err.println("Error deleting image file: " + e.getMessage());
                        AlertUtil.showWarning("File Error", "University deleted, but could not remove the associated image file from disk.");
                    }
                }
            }


        } else {
            AlertUtil.showInfo("Cancelled","Deletion cancelled.");
        }
    }
    private void handleWebsiteLink(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackClick() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }


    private Button createIconButton(String imagePath) {
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
        icon.setFitHeight(16);
        icon.setFitWidth(16);
        Button button = new Button();
        button.setGraphic(icon);
        button.setStyle("-fx-background-color: transparent; -fx-padding: 3;");
        return button;
    }

}
