package com.example;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class GridController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label cityLabel;
    @FXML
    private Label zipcodeLabel;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;

    private List<String[]> personData;
    private int currentIndex = 0;

    @FXML
    public void initialize() {
        personData = fetchPersonData();
        if (!personData.isEmpty()) {
            displayRecord(currentIndex);
        }
    }

    private List<String[]> fetchPersonData() {
        List<String[]> data = new ArrayList<>();
        String query = "SELECT name, city, zipcode FROM person";
        try (Connection connection = Database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                String name    = resultSet.getString("name");
                String city    = resultSet.getString("city");
                String zipcode = resultSet.getString("zipcode");
                data.add(new String[]{name, city, zipcode});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    private void displayRecord(int index) {
        String[] person = personData.get(index);
        nameLabel.setText(person[0]);
        cityLabel.setText(person[1]);
        zipcodeLabel.setText(person[2]);
    }

    @FXML
    void nextRecord(ActionEvent event) {
        if (currentIndex < personData.size() - 1) {
            currentIndex++;
            displayRecord(currentIndex);
        }
    }

    @FXML
    void prevRecord(ActionEvent event) {
        if (currentIndex > 0) {
            currentIndex--;
            displayRecord(currentIndex);
        }
    }

    @FXML
    void switchVScene(ActionEvent event) throws IOException {
        App.setRoot("vboxscene");
    }
}
