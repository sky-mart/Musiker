package com.mocsmart.musiker;

import javafx.application.Application;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


public class Ui extends Application {
    private Stage stage;
    private Scene authScene;
    private Scene mainScene;
    private TextField searchField;
    private Label stateLabel;
    private ComboBox searchMode;
    private Button downloadButton;
    private ListView albumListView;
    private ListView trackListView;
    private WebEngine webEngine;

    private Label titleLabel;
    private ProgressBar progressBar;
    private Slider volumeBar;
    private Label volumeLabel;

    private Properties props;
    private final String accessGrantedPropertyName = "ACCESS_GRANTED";
    private final String accessTokenPropertyName = "ACCESS_TOKEN";
    private final String defaultWidthPropertyName = "DEFAULT_WIDTH";
    private final String defaultHeightPropertyName = "DEFAULT_HEIGHT";
    private final String defaultDirectoryPropertyName = "DEFAULT_DIR";

    private int DEFAULT_WIDTH = 500;
    private int DEFAULT_HEIGHT = 400;
    private String saveDir;

    private Map<String, List<String>> cache = new HashMap<String, List<String>>(); // key - album name, value - list of tracks

    private MediaPlayer player;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        loadProperties();
        DEFAULT_WIDTH = Integer.valueOf(props.getProperty(defaultWidthPropertyName));
        DEFAULT_HEIGHT = Integer.valueOf(props.getProperty(defaultHeightPropertyName));

        createAuthScene();
        createMainScene();

        Downloader.setUi(this);
        String accessGranted = props.getProperty(accessGrantedPropertyName);
        if (accessGranted.equalsIgnoreCase("NO")) {
            stage.setScene(authScene);
            webEngine.load("https://oauth.vk.com/authorize?" +
                    "client_id=4544975&" +
                    "scope=offline,audio&" +
                    "redirect_uri=https://oauth.vk.com/blank.html&" +
                    "display=popup&" +
                    "v=5.24&" +
                    "response_type=token");
        } else if (accessGranted.equalsIgnoreCase("YES")) {
            String accessToken = props.getProperty(accessTokenPropertyName);
            Downloader.setVkAccessToken(accessToken);
            stage.setScene(mainScene);
        }

        primaryStage.show();
    }

    private void loadProperties()
    {
        props = new Properties();

        try {
            URI uri = new URI(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
            uri = uri.resolve("musiker.ps");
            InputStream is = new FileInputStream(uri.toString());
            props.load(is);
            saveDir = props.getProperty(defaultDirectoryPropertyName);
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private void saveToken(String accessToken)
    {
        props.setProperty(accessGrantedPropertyName, "YES");
        props.setProperty(accessTokenPropertyName, accessToken);
        try {
            URI uri = new URI(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
            uri = uri.resolve("musiker.ps");
            OutputStream os = new FileOutputStream(uri.toString());

            props.store(os, "Musiker Settings");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void createAuthScene()
    {
        final WebView browser = new WebView();
        webEngine = browser.getEngine();

        StackPane root = new StackPane();
        root.getChildren().add(browser);
        authScene = new Scene(root/*, DEFAULT_WIDTH, DEFAULT_HEIGHT*/);

        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<State>() {
                    public void changed(ObservableValue ov, State oldState, State newState) {
                        if (newState == State.SUCCEEDED) {
                            String location = webEngine.getLocation();

                            if (location.contains("https://oauth.vk.com/blank.html#")) {
                                String[] info = getAccessInfo(location);
                                Downloader.setVkAccessToken(info[0]);

                                saveToken(info[0]);
                                stage.setScene(mainScene);
                            }
                        }
                    }
                });
    }

    private String[] getAccessInfo(String location)
    {
        String[] info = new String[3];
        int from, to;

        from = location.indexOf("access_token=") + "access_token=".length();
        to = location.indexOf("&", from);
        info[0] = location.substring(from, to);

        from = location.indexOf("expires_in=", to) + "expires_in=".length();
        to = location.indexOf("&", from);
        info[1] = location.substring(from, to);

        from = location.indexOf("user_id=", to) + "user_id=".length();
        to = location.length();
        info[2] = location.substring(from, to);

        return info;
    }

    private void createMainScene()
    {
        TabPane tabPane     = new TabPane();
        Tab mainTab         = new Tab("Main");
        try {
            mainTab.setContent((Node) FXMLLoader.load(getClass().getResource("main.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Tab downloadsTab    = new Tab("Downloads");
        Tab optionsTab      = new Tab("Options");
        tabPane.getTabs().addAll(mainTab, downloadsTab, optionsTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        mainScene = new Scene(tabPane, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

}

