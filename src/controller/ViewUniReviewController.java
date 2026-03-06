package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.University;
import model.UniversityReview;
import model.UserSession;
import util.AlertUtil;
import util.DatabaseConnector;
import util.SceneManager;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.ResourceBundle;

public class ViewUniReviewController extends BaseController implements Initializable {

    @FXML private Label uniName;
    @FXML private ImageView uniImage; // Added this field
    @FXML private TableView<UniversityReview> reviewTable;
    @FXML private TableColumn<UniversityReview, String> contentCol;
    @FXML private TableColumn<UniversityReview, LocalDate> dateCol;
    @FXML private TableColumn<UniversityReview, Integer> ratingCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    private University university;

    private final ObservableList<UniversityReview> reviewList = FXCollections.observableArrayList();
    private FilteredList<UniversityReview> filteredReviews;


    public void initData(University fullUni) {
        university = fullUni;
        uniName.setText(university.getUniName());

        String imagePath = university.getUniPicturePath();
        Image image = null;
        if (imagePath != null && !imagePath.isEmpty()) {
            try {

                File imageFile = new File("university_images/" + imagePath);
                if (imageFile.exists() && imageFile.isFile()) {
                    image = new Image(imageFile.toURI().toString());
                } else {
                    System.err.println("University image file not found: " + imageFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Could not load university image: " + e.getMessage());
            }
        }

        if (image == null || image.isError()) {
            try {

                image = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/icons/placeholder.png")));
            } catch (Exception e) {
                System.err.println("CRITICAL: Could not load placeholder image: " + e.getMessage());

                uniImage.setImage(null);
            }
        }
        if (image != null) {
            uniImage.setImage(image);
        }

        loadReviewsFromDatabase();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHeader();
        setupTable();
        setupSearch();
        setupSortOptions();

    }

    private void setupTable() {
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("datePosted"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        contentCol.setSortable(false);
        dateCol.setSortable(false);
        ratingCol.setSortable(false);


        filteredReviews = new FilteredList<>(reviewList, p -> true);
        reviewTable.setItems(filteredReviews);

    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            applyFiltersAndSort();
        });
    }

    private void setupSortOptions() {
        sortComboBox.setItems(FXCollections.observableArrayList("Date (Newest)", "Date (Oldest)", "Rating (High → Low)", "Rating (Low → High)"));
        sortComboBox.setValue("Date (Newest)");
        sortComboBox.setOnAction(e -> applyFiltersAndSort());
    }


    private void applyFiltersAndSort() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        filteredReviews.setPredicate(review -> {
            if (keyword.isEmpty()) return true;
            return (review.getContent() != null && review.getContent().toLowerCase().contains(keyword))
                    || String.valueOf(review.getRating()).contains(keyword)
                    || (review.getDatePosted() != null && review.getDatePosted().toString().contains(keyword));
        });


        String selectedSort = sortComboBox.getValue();
        if (selectedSort == null) return;

        Comparator<UniversityReview> comparator = switch (selectedSort) {
            case "Date (Newest)" -> Comparator.comparing(UniversityReview::getDatePosted, Comparator.nullsLast(Comparator.reverseOrder()));
            case "Date (Oldest)" -> Comparator.comparing(UniversityReview::getDatePosted, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Rating (High → Low)" -> Comparator.comparingInt(UniversityReview::getRating).reversed();
            case "Rating (Low → High)" -> Comparator.comparingInt(UniversityReview::getRating);
            default -> (r1, r2) -> 0;
        };
        FXCollections.sort(reviewList, comparator);
        reviewTable.refresh();
    }


    private void loadReviewsFromDatabase() {
        reviewList.clear();
        String sql = "SELECT UniReviewID, StudentID, Content, Rating, DatePosted " +
                "FROM UniversityReview WHERE UniversityID = ?";

        if (university == null) {
            System.err.println("University object is null. Cannot load reviews.");
            return;
        }

        try (Connection conn = DatabaseConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {


            pstmt.setInt(1, university.getUniversityID());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Date sqlDate = rs.getDate("DatePosted");
                LocalDate date = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                reviewList.add(new UniversityReview(
                        rs.getInt("UniReviewID"),
                        rs.getString("Content"),
                        rs.getInt("Rating"),
                        date
                ));
            }
            applyFiltersAndSort();

        } catch (SQLException e) {
            AlertUtil.showError("Unexpected Error", "Could not load university reviews.");
            e.printStackTrace();
        } catch (NullPointerException e) {
            AlertUtil.showError("Database Error", "Failed to get database connection. Is the DatabaseManager initialized?");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLoginRegister(){
        SceneManager.switchTo("/view/login_view.fxml");
    }

    @FXML
    private void handleBackClick() {
        SceneManager.switchTo("/view/university_selection.fxml");
    }

    @FXML
    private void handleHome() {
        SceneManager.switchTo("/view/dashboard.fxml");
    }

    @FXML private void handleViewProfile() { SceneManager.switchTo("/view/view_profile.fxml"); }
    @FXML private void handleLogout() {
        UserSession.getInstance().logout();
        AlertUtil.showInfo("Logout", "You have been logged out.");
        SceneManager.switchTo("/view/dashboard.fxml");
    }
}

