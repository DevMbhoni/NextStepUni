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
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import model.Bursary;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;

import java.util.ResourceBundle;

public class ManageBursariesController extends BaseController implements Initializable {


    @FXML
    private TableView<Bursary> bursaryTableView;
    @FXML
    private TableColumn<Bursary, String> bursaryNameColumn;
    @FXML
    private TableColumn<Bursary, LocalDate> deadlineColumn;
    @FXML
    private TableColumn<Bursary, String> websiteColumn;
    @FXML
    private TableColumn<Bursary, Void> actionColumn;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Button addButton;

    private final ObservableList<Bursary> bursaryList = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        loadBursariesFromDatabase();
        configureTableColumns();
        setupFilterAndSort();
        sortComboBox.getItems().addAll("Default", "By Name", "By Deadline");
        sortComboBox.setValue("Default");
    }

    private void loadBursariesFromDatabase() {
        bursaryList.clear();
        String sql = "SELECT BursaryID, BurName, ApplicationDeadline, Description, WebsiteLink FROM Bursary";
        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                bursaryList.add(new Bursary(
                        rs.getInt("BursaryID"),
                        rs.getString("BurName"),
                        rs.getDate("ApplicationDeadline").toLocalDate(),
                        rs.getString("Description"),
                        rs.getString("WebsiteLink")
                ));
            }
        } catch (Exception e) {
            System.err.println("Error loading bursaries from the database.");
            e.printStackTrace();
        }
    }

    private void configureTableColumns() {
        bursaryNameColumn.setCellValueFactory(new PropertyValueFactory<>("burName"));
        deadlineColumn.setCellValueFactory(new PropertyValueFactory<>("applicationDeadline"));
        websiteColumn.setCellFactory(createWebsiteCellFactory());
        actionColumn.setCellFactory(createActionCellFactory());
        bursaryNameColumn.setSortable(false);
        deadlineColumn.setSortable(false);
        websiteColumn.setSortable(false);
        actionColumn.setSortable(false);


    }

    private void setupFilterAndSort() {
        FilteredList<Bursary> filteredData = new FilteredList<>(bursaryList, b -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(bursary -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return bursary.getBurName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<Bursary> sortedData = new SortedList<>(filteredData);

        sortComboBox.valueProperty().addListener((_, _, newVal) -> {
            sortedData.setComparator((u1, u2) -> {
                if ("By Name".equals(newVal)) {
                    return u1.getBurName().compareToIgnoreCase(u2.getBurName());
                } else if ("By Deadline".equals(newVal)) {
                    return u1.getApplicationDeadline().compareTo(u2.getApplicationDeadline());
                } else {
                    return 0;
                }
            });
        });

        bursaryTableView.setItems(sortedData);
    }

    private Callback<TableColumn<Bursary, String>, TableCell<Bursary, String>> createWebsiteCellFactory() {
        return param -> new TableCell<>() {
            private final Hyperlink link = new Hyperlink("[Link]");
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Bursary bursary = getTableView().getItems().get(getIndex());
                    link.setOnAction(event -> handleWebsiteLink(bursary.getWebsiteLink()));
                    setGraphic(link);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private Callback<TableColumn<Bursary, Void>, TableCell<Bursary, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button editButton = createIconButton("/images/icons/edit_icon.png");
            private final Button deleteButton = createIconButton("/images/icons/delete_icon.png");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                pane.setAlignment(Pos.CENTER);
                editButton.setOnAction(event -> {
                    Bursary bursary = getTableView().getItems().get(getIndex());
                    handleEditBursary(bursary);

                });
                deleteButton.setOnAction(event -> {
                    Bursary bursary = getTableView().getItems().get(getIndex());
                    handleDeleteBursary(bursary);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        };
    }
    @FXML
    private void handleAddBursary(ActionEvent event) {
        SceneManager.switchTo("/view/add_bursary.fxml");
    }

    @FXML
    private void handleBackClick(){
        SceneManager.switchTo("/view/dashboard.fxml");
    }
    private void handleEditBursary(Bursary bursary) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/update_bursary.fxml"));
            Parent root = loader.load();
            UpdateBursaryController controller = loader.getController();
            controller.initData(bursary);
            Stage stage = (Stage) bursaryTableView.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteBursary(Bursary bursary) {
        boolean confirmed = AlertUtil.showCustomConfirmation(
                "Confirm Deletion",
                "Delete Bursary: " + bursary.getBurName(),
                "PERMANENTLY DELETE '" + bursary.getBurName() + "'?\n\nThis will remove:\n- The bursary itself.\n- All associated reviews.\n- All links to qualifications.\n\nThis action cannot be undone.",
                "Delete Permanently",
                "Cancel"
        );

        if (!confirmed) {
            AlertUtil.showInfo("Cancelled", "Deletion cancelled.");
            return;
        }

        Connection conn = null;
        boolean success = false;
        try {
            conn = DatabaseConnector.getInstance().getConnection();
            conn.setAutoCommit(false);
            String deleteReviewsSql = "DELETE FROM BursaryReview WHERE BursaryID = ?";
            try (PreparedStatement stmtReviews = conn.prepareStatement(deleteReviewsSql)) {
                stmtReviews.setInt(1, bursary.getBursaryID());
                int reviewsDeleted = stmtReviews.executeUpdate();
                System.out.println("Deleted " + reviewsDeleted + " reviews associated with BursaryID: " + bursary.getBursaryID());
            } catch (SQLException e) {
                System.err.println("Error deleting reviews: " + e.getMessage());
                throw e;
            }

            String deleteLinksSql = "DELETE FROM BursaryQualification WHERE BursaryID = ?";
            try (PreparedStatement stmtLinks = conn.prepareStatement(deleteLinksSql)) {
                stmtLinks.setInt(1, bursary.getBursaryID());
                int linksDeleted = stmtLinks.executeUpdate();
                System.out.println("Deleted " + linksDeleted + " qualification links associated with BursaryID: " + bursary.getBursaryID());
            } catch (SQLException e) {
                System.err.println("Error deleting qualification links: " + e.getMessage());
                throw e;
            }

            String deleteBursarySql = "DELETE FROM Bursary WHERE BursaryID = ?";
            try (PreparedStatement stmtBursary = conn.prepareStatement(deleteBursarySql)) {
                stmtBursary.setInt(1, bursary.getBursaryID());
                int affectedRows = stmtBursary.executeUpdate();
                if (affectedRows > 0) {
                    success = true;
                    System.out.println("Successfully deleted Bursary with ID: " + bursary.getBursaryID());
                } else {
                    System.err.println("Bursary with ID " + bursary.getBursaryID() + " not found for deletion.");
                    AlertUtil.showWarning("Deletion Warning", "Could not find the bursary (ID: " + bursary.getBursaryID() + ") to delete. It might have already been removed.");

                    success = true;
                }
            } catch (SQLException e) {
                System.err.println("Error deleting the bursary itself: " + e.getMessage());
                throw e;
            }

            conn.commit();
        } catch (SQLException e) {
            success = false;
            System.err.println("Database transaction failed during bursary deletion: " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("Database Error", "Failed to delete bursary due to a database error. Check related data.");
            try {
                if (conn != null) {
                    System.err.println("Rolling back transaction...");
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("CRITICAL: Error rolling back transaction: " + ex.getMessage());
            }
        } catch (NullPointerException e) {
            success = false;
            System.err.println("Error getting database connection (NullPointerException).");
            e.printStackTrace();
            AlertUtil.showError("Connection Error", "Could not connect to the database.");
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Error restoring auto-commit mode: " + e.getMessage());
            }
        }

        if (success) {
            bursaryList.remove(bursary);
            AlertUtil.showInfo("Deleted", "'" + bursary.getBurName() + "' and all associated data were successfully removed.");
        }
    }


    private void handleWebsiteLink(String url) {
        try {
            String fullUrl = url.toLowerCase().startsWith("http") ? url : "https://" + url;
            Desktop.getDesktop().browse(new URI(fullUrl));
        } catch (IOException | URISyntaxException e) {
            System.err.println("Failed to open link: " + e.getMessage());
        }
    }

    private Button createIconButton(String imagePath) {
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
            icon.setFitHeight(16);
            icon.setFitWidth(16);
            Button button = new Button();
            button.setGraphic(icon);
            button.setStyle("-fx-background-color: transparent; -fx-padding: 3;");
            button.setFont(Font.font(14));
            return button;
        } catch (Exception e) {
            System.err.println("Could not load icon: " + imagePath);
            String text = imagePath.contains("edit") ? "E" : "D";
            return new Button(text);
        }
    }
}
