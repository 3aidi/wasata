package com.mylibrary;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LibraryUI extends Application {

    private TableView<Book> tableView;
    private TextField titleField;
    private TextField authorField;
    private CheckBox availableCheckBox;
    private TextField searchField;

    public static void main(String[] args) {
        launch(args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(Stage primaryStage) {
        tableView = new TableView<>();
        tableView.setId("tableView");
        titleField = new TextField();
        titleField.setId("titleField");
        titleField.setPromptText("Title");
        authorField = new TextField();
        authorField.setPromptText("Author");
        authorField.setId("authorField");
        availableCheckBox = new CheckBox("Available");
        availableCheckBox.setId("availableCheckBox");
        searchField = new TextField();
        searchField.setPromptText("Search by title...");
        searchField.setId("searchField");

        TableColumn<Book, Number> idColumn = new TableColumn<>("Book ID");
        idColumn.setPrefWidth(100);
        idColumn.setCellValueFactory(cellData -> cellData.getValue().bookIdProperty());

        TableColumn<Book, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setPrefWidth(200);
        titleColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());

        TableColumn<Book, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setPrefWidth(150);
        authorColumn.setCellValueFactory(cellData -> cellData.getValue().authorProperty());

        TableColumn<Book, Boolean> availableColumn = new TableColumn<>("Available");
        availableColumn.setPrefWidth(100);
        availableColumn.setCellValueFactory(cellData -> cellData.getValue().availableProperty());

        // TODO 9: Add columns to tableView
        tableView.getColumns().addAll(idColumn, titleColumn, authorColumn, availableColumn);

        Button searchButton  = new Button("Search");       searchButton.setId("searchButton");
        Button insertButton  = new Button("Insert");       insertButton.setId("insertButton");
        Button updateButton  = new Button("Update");       updateButton.setId("updateButton");
        Button deleteButton  = new Button("Delete");       deleteButton.setId("deleteButton");
        Button refreshButton = new Button("Refresh");      refreshButton.setId("refreshButton");
        Button sortButton    = new Button("Sort by Title"); sortButton.setId("sortButton");

        searchButton.setOnAction(e  -> searchBooks());
        insertButton.setOnAction(e  -> insertBook());
        updateButton.setOnAction(e  -> updateBook());
        deleteButton.setOnAction(e  -> deleteBook());
        refreshButton.setOnAction(e -> refreshTable());
        sortButton.setOnAction(e    -> sortBooks());

        HBox buttonBox = new HBox(10, searchButton, insertButton, updateButton,
                                      deleteButton, refreshButton, sortButton);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(10, searchField, titleField, authorField,
                                availableCheckBox, buttonBox, tableView);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Library Management System");
        primaryStage.setScene(scene);
        primaryStage.show();

        refreshTable();
    }

    private ObservableList<Book> loadAllBooks() {
        ObservableList<Book> bookList = FXCollections.observableArrayList();

        // TODO 11: SQL query to select all books
        String query = "SELECT book_id, title, author, available FROM Books";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int     bookId    = rs.getInt("book_id");
                String  title     = rs.getString("title");
                String  author    = rs.getString("author");
                boolean available = rs.getBoolean("available");
                bookList.add(new Book(bookId, title, author, available));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookList;
    }

    private void refreshTable() {
        tableView.setItems(loadAllBooks());
    }

    private void searchBooks() {
        String searchText = searchField.getText().toLowerCase();

        if (searchText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a title to search.");
            return;
        }

        ObservableList<Book> filteredBooks = FXCollections.observableArrayList();

        for (Book book : loadAllBooks()) {
            if (book.getTitle().toLowerCase().contains(searchText)) {
                // TODO 13: add matching book to filteredBooks
                filteredBooks.add(book);
            }
        }

        tableView.setItems(filteredBooks);

        if (filteredBooks.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No books found matching the search term.");
        }
    }

    private void insertBook() {
        String  title     = titleField.getText();
        String  author    = authorField.getText();
        boolean available = availableCheckBox.isSelected();

        if (title.isEmpty() || author.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Title and Author cannot be empty.");
            return;
        }

        String query = "INSERT INTO Books (title, author, available) VALUES (?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.setBoolean(3, available);

            // TODO 12: execute the prepared statement
            pstmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Book inserted successfully.");
            clearInputFields();
            refreshTable();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to insert the book.");
        }
    }

    private void updateBook() {
        // TODO 14: retrieve selected book
        Book selectedBook = tableView.getSelectionModel().getSelectedItem();

        if (selectedBook == null) {
            showAlert(Alert.AlertType.ERROR, "Selection Error", "No book selected for update.");
            return;
        }

        String  newTitle       = titleField.getText();
        String  newAuthor      = authorField.getText();
        boolean newAvailability = availableCheckBox.isSelected();

        if (newTitle.isEmpty() || newAuthor.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Title and Author cannot be empty.");
            return;
        }

        String query = "UPDATE Books SET title = ?, author = ?, available = ? WHERE book_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newTitle);
            pstmt.setString(2, newAuthor);
            pstmt.setBoolean(3, newAvailability);
            pstmt.setInt(4, selectedBook.getBookId());

            pstmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book updated successfully.");
            clearInputFields();
            refreshTable();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update the book.");
        }
    }

    private void deleteBook() {
        Book selectedBook = tableView.getSelectionModel().getSelectedItem();

        if (selectedBook == null) {
            showAlert(Alert.AlertType.ERROR, "Selection Error", "No book selected for deletion.");
            return;
        }

        // TODO 15: SQL query to delete a book
        String query = "DELETE FROM Books WHERE book_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, selectedBook.getBookId());
            pstmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book deleted successfully.");
            refreshTable();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete the book.");
        }
    }

    private void sortBooks() {
        // TODO 16: retrieve books and sort by title
        ObservableList<Book> books = tableView.getItems();
        FXCollections.sort(books, Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearInputFields() {
        titleField.clear();
        authorField.clear();
        availableCheckBox.setSelected(false);
    }

    private Connection connect() throws SQLException {
        // TODO 10: database credentials
        String url      = "jdbc:mysql://localhost:3306/LibraryDB";
        String user     = "root";
        String password = "password";
        return DriverManager.getConnection(url, user, password);
    }
}
