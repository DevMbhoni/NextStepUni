package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Bursary;
import model.BursaryReview;
import util.DatabaseConnector;
import util.SceneManager;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ViewBursaryReviewController extends BaseController implements Initializable {

    @FXML
    private Label BurName;

    @FXML
    private TableView<BursaryReview> reviewTable;

    @FXML
    private TableColumn<BursaryReview, String> contentCol;

    @FXML
    private TableColumn<BursaryReview, LocalDate> dateCol;

    @FXML
    private TableColumn<BursaryReview, Integer> ratingCol;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    private final ObservableList<BursaryReview> masterReviewList = FXCollections.observableArrayList();
    private Bursary selectedBursary;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupHeader();
        setupTableColumns();
        setupListeners();

        sortComboBox.getItems().addAll("Default", "Newest First", "Highest Rating");
        sortComboBox.setValue("Default");
    }

    public void initData(Bursary bursary) {
        this.selectedBursary = bursary;
        if (bursary != null) {
            BurName.setText(bursary.getBurName());
            loadReviewsFromDatabase(bursary.getBursaryID());
        }
    }

    private void setupTableColumns() {
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePosted"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        contentCol.setSortable(false);
        dateCol.setSortable(false);
        ratingCol.setSortable(false);

    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateTable());
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateTable());
    }

    private void loadReviewsFromDatabase(int bursaryId) {
        masterReviewList.clear();

        String query = "SELECT BursaryReviewID, Content, DatePosted, Rating " +
                "FROM BursaryReview WHERE BursaryID = " + bursaryId;

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int reviewId = rs.getInt("BursaryReviewID");
                String content = rs.getString("Content");
                LocalDate datePosted = rs.getDate("DatePosted").toLocalDate();
                int rating = rs.getInt("Rating");

                masterReviewList.add(new BursaryReview(reviewId, bursaryId, content, datePosted, rating));
            }

        } catch (SQLException e) {
            System.err.println("Error loading reviews: " + e.getMessage());
        }

        updateTable();
    }

    private void updateTable() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase() : "";

        List<BursaryReview> filtered = masterReviewList.stream()
                .filter(r -> r.getContent().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        String sortOption = sortComboBox.getValue();
        if ("Newest First".equals(sortOption)) {
            filtered.sort(Comparator.comparing(BursaryReview::getDatePosted).reversed());
        } else if ("Highest Rating".equals(sortOption)) {
            filtered.sort(Comparator.comparing(BursaryReview::getRating).reversed());
        }

        reviewTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleBackClick() {
        SceneManager.switchTo("/view/select_review_bursary.fxml");
    }

    @FXML
    private void handleLoginRegister() {
        SceneManager.switchTo("/view/login_view.fxml");
    }
}
