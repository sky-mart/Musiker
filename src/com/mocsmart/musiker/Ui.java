package com.mocsmart.musiker;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    private TextField artistField;
    private Label stateLabel;
    private ToggleGroup downloadMode;
    private ListView albumListView;
    private ListView trackListView;
    private WebEngine webEngine;

    private Properties props;
    private final String accessGrantedPropertyName = "ACCESS_GRANTED";
    private final String accessTokenPropertyName = "ACCESS_TOKEN";
    private final String defaultWidthPropertyName = "DEFAULT_WIDTH";
    private final String defaultHeightPropertyName = "DEFAULT_HEIGHT";
    private final String defaultDirectoryPropertyName = "DEFAULT_DIR";

    private int DEFAULT_WIDTH = 500;
    private int DEFAULT_HEIGHT = 400;
    private String saveDir;

    private Map<String, List<String>> cash = new HashMap<String, List<String>>(); // key - album name, value - list of tracks

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
        authScene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

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
        VBox main = new VBox();
        HBox middle = new HBox();
        HBox rbuttons = new HBox();
        HBox top = new HBox();
        VBox bottom = new VBox();

        main.getChildren().add(top);
        main.getChildren().add(bottom);

        artistField = new TextField("Input artist");
        Button searchButton = new Button("Search");
        stateLabel = new Label();
        top.getChildren().add(artistField);
        top.getChildren().add(searchButton);
        top.getChildren().add(stateLabel);

        bottom.getChildren().add(middle);
        bottom.getChildren().add(rbuttons);

        albumListView = new ListView();
        albumListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        middle.getChildren().add(albumListView);

        trackListView = new ListView();
        trackListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        middle.getChildren().add(trackListView);

        downloadMode = new ToggleGroup();
        RadioButton downloadTracks = new RadioButton("Tracks");
        downloadTracks.setSelected(true);
        downloadTracks.setToggleGroup(downloadMode);
        downloadTracks.setUserData("Tracks");
        RadioButton downloadAlbums = new RadioButton("Albums");
        downloadAlbums.setToggleGroup(downloadMode);
        downloadAlbums.setUserData("Albums");
        RadioButton downloadAll = new RadioButton("All");
        downloadAll.setToggleGroup(downloadMode);
        downloadAll.setUserData("All");

        Button downloadButton = new Button("Download");
        Button playButton = new Button("Play");

        rbuttons.getChildren().add(downloadAlbums);
        rbuttons.getChildren().add(downloadTracks);
        rbuttons.getChildren().add(downloadAll);
        rbuttons.getChildren().add(downloadButton);
        rbuttons.getChildren().add(playButton);

        artistField.setOnKeyPressed(new EventHandler<KeyEvent>(){
            @Override
            public void handle(KeyEvent ke) {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    search();
                }
            }
        });

        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                search();
            }
        });

        albumListView.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    public void changed(ObservableValue<? extends String> observable,
                                        String oldAlbum, final String newAlbum) {
                        if (observable.getValue() != null) {
                            if (cash.containsKey(newAlbum)) {
                                trackListView.setItems((ObservableList) cash.get(newAlbum));
                            } else {
                                final String artist = artistField.getText();

                                final Task<ObservableList<String>> trackListTask = new Task<ObservableList<String>>() {
                                    @Override
                                    protected ObservableList<String> call() throws Exception {
                                        updateMessage("Getting list of tracks...");
                                        List<String> tracks = Downloader.getTracks(artist, newAlbum);
                                        updateMessage("Got list of tracks");
                                        return FXCollections.observableArrayList(tracks);
                                    }
                                };
                                stateLabel.textProperty().bind(trackListTask.messageProperty());
                                trackListTask.stateProperty().addListener(new ChangeListener<State>() {
                                    @Override
                                    public void changed(ObservableValue<? extends State> observableValue, State state, State state2) {
                                        if (state2 == State.SUCCEEDED) {
                                            stateLabel.textProperty().unbind();
                                            ObservableList tracks = trackListTask.getValue();
                                            trackListView.setItems(tracks);
                                            cash.put(newAlbum, tracks);
                                        }
                                    }
                                });
                                new Thread(trackListTask).start();
                            }
                        }
                    }
                });

        downloadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final String artist = artistField.getText();
                List<String> chosenAlbums = albumListView.getSelectionModel().getSelectedItems();
                final Map<String, List<String>> albumTracks = new HashMap<String, List<String>>();

                String selectedMode = downloadMode.getSelectedToggle().getUserData().toString();
                if (selectedMode.equalsIgnoreCase("Tracks")) {
                    if (chosenAlbums.size() > 1) {
                        System.out.println("Choose only one album, pls");
                        return;
                    } else {
                        String album = chosenAlbums.get(0);
                        List<String> titles = trackListView.getSelectionModel().getSelectedItems();
                        albumTracks.put(album, titles);
                    }
                } else if (selectedMode.equalsIgnoreCase("Albums")) {
                    for (String album : chosenAlbums) {
                        albumTracks.put(album, cash.get(album));
                    }
                } else if (selectedMode.equalsIgnoreCase("All")) {
                    List<String> albums = albumListView.getItems();
                    for (String album : albums) {
                        albumTracks.put(album, cash.get(album));
                    }
                }

                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Choose directory to save tracks");
                if (saveDir != null) {
                    chooser.setInitialDirectory(new File(saveDir));
                }
                final File selectedDirectory = chooser.showDialog(stage);
                if (selectedDirectory == null) return;
                saveDir = selectedDirectory.getAbsolutePath() + "/";

                DownloadSongsTask downloadSongsTask = new DownloadSongsTask(artist, albumTracks, saveDir);
                stateLabel.textProperty().bind(downloadSongsTask.messageProperty());
                new Thread(downloadSongsTask).start();
            }
        });

        playButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Button source = (Button) actionEvent.getSource();
                if (source.getText().equalsIgnoreCase("Play")) {
                    String artist = artistField.getText();
                    List<String> titles = trackListView.getSelectionModel().getSelectedItems();
                    if (titles.size() > 1) {
                        System.out.println("Select only one track");
                        return;
                    }

                    player = new MediaPlayer(new Media(Downloader.downloadUrl(titles.get(0))));
                    player.play();
                    source.setText("Stop");
                } else if (source.getText().equalsIgnoreCase("Stop")) {
                    player.stop();
                    source.setText("Play");
                }
            }
        });

        mainScene = new Scene(main, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    private void search()
    {
        albumListView.setItems(null);
        trackListView.setItems(null);

        final String artist = artistField.getText();

        final Task<ObservableList<String>> albumListTask = new Task<ObservableList<String>>() {
            @Override
            protected ObservableList<String> call() throws Exception {
                updateMessage("Getting list of albums...");
                List<String> albums = Downloader.getAlbums(artist);
                updateMessage("Got list of albums");
                return FXCollections.observableArrayList(albums);
            }
        };
        stateLabel.textProperty().bind(albumListTask.messageProperty());
        albumListTask.stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> observableValue, State state, State state2) {
                if (state2 == State.SUCCEEDED) {
                    stateLabel.textProperty().unbind();
                    albumListView.setItems(albumListTask.getValue());
                    cash.clear();
                }
            }
        });
        new Thread(albumListTask).start();
    }
}

