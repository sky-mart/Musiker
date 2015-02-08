package com.mocsmart.musiker;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class DownloadsController {

    @FXML
    private TextField urlField;
    @FXML
    private Button downloadButton;
    @FXML
    private TableView table;

    @FXML
    private Button pauseButton;
    @FXML
    private Button resumeButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button clearButton;
}
