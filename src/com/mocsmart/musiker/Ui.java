package com.mocsmart.musiker;

import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class Ui extends Application {
    private Stage stage;
    private Scene authScene;
    private Scene mainScene;
    private WebEngine webEngine;


    private Properties props;
    private final String accessGrantedPropertyName = "ACCESS_GRANTED";
    private final String accessTokenPropertyName = "ACCESS_TOKEN";
    private final String defaultWidthPropertyName = "DEFAULT_WIDTH";
    private final String defaultHeightPropertyName = "DEFAULT_HEIGHT";
    private final String defaultDirectoryPropertyName = "DEFAULT_DIR";

    private int DEFAULT_WIDTH = 600;
    private int DEFAULT_HEIGHT = 500;
    private String saveDir;


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
        tabPane.setTabMinWidth(80);

        //tabPane.getStylesheets().add(this.getClass().getResource("style.css").toExternalForm());
        mainScene = new Scene(tabPane, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

}

