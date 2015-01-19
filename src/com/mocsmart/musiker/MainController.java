package com.mocsmart.musiker;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static java.lang.Math.abs;

public class MainController implements Initializable {
    @FXML
    Parent root;

    @FXML
    Button playButton;
    @FXML
    Button pauseButton;
    @FXML
    Label titleLabel;
    @FXML
    Label timeLeftLabel;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label volumeLabel;
    @FXML
    Slider volumeBar;

    @FXML
    TextField searchField;
    @FXML
    ComboBox<String> searchModeCombo;
    @FXML
    Button downloadButton;
    @FXML
    ComboBox<String> downloadModeCombo;

    @FXML
    ListView albumListView;
    @FXML
    ListView trackListView;

    @FXML
    Label stateLabel;

    private MediaPlayer player;
    private Map<String, List<String>> cache = new HashMap<String, List<String>>(); // key - album name, value - list of tracks
    private String saveDir;
    private String searchMode = "Artist";
    private String downloadMode = "Tracks";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        albumListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        albumListView.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    public void changed(ObservableValue<? extends String> observable,
                                        String oldAlbum, final String newAlbum) {
                        if (observable.getValue() != null) {
                            if (cache.containsKey(newAlbum)) {
                                trackListView.setItems((ObservableList) cache.get(newAlbum));
                            } else {
                                final String artist = searchField.getText();

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
                                trackListTask.stateProperty().addListener(new ChangeListener<Worker.State>() {
                                    @Override
                                    public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State state2) {
                                        if (state2 == Worker.State.SUCCEEDED) {
                                            stateLabel.textProperty().unbind();
                                            ObservableList tracks = trackListTask.getValue();
                                            trackListView.setItems(tracks);
                                            cache.put(newAlbum, tracks);
                                        }
                                    }
                                });
                                new Thread(trackListTask).start();
                            }
                        }
                    }
                });
        trackListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        volumeBar.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                volumeLabel.setText(String.valueOf(number2.intValue()) + "%");
                player.setVolume(number2.doubleValue() / 100.0);
            }
        });

        searchModeCombo.setItems(FXCollections.observableArrayList("Artist", "Tracks"));
        searchModeCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                searchMode = s2;
            }
        });

        downloadModeCombo.setItems(FXCollections.observableArrayList("Tracks", "Albums"));
        downloadModeCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                downloadMode = s2;
            }
        });
    }

    @FXML
    private void play() {
        if (playButton.getText().equalsIgnoreCase("Play")) {
            String artist = searchField.getText();
            List<String> titles = trackListView.getSelectionModel().getSelectedItems();
            if (titles.size() > 1) {
                System.out.println("Select only one track");
                return;
            }

            String title = artist + " - " + titles.get(0);
            player = new MediaPlayer(new Media(Downloader.downloadUrl(title)));

            titleLabel.setText(title);
            player.currentTimeProperty().addListener(new ChangeListener<Duration>() {
                @Override
                public void changed(ObservableValue<? extends Duration> observableValue, Duration duration, Duration duration2) {
                    Duration total = player.getTotalDuration();
                    Duration left = duration2.subtract(total);

                    double progress = duration2.toMillis() / total.toMillis();
                    progressBar.setProgress(progress);

                    int secs = (int) left.toSeconds();
                    int mins = secs / 60;
                    secs %= 60;
                    secs = abs(secs);
                    String minus = (duration2.lessThan(total) && mins == 0) ? "-" : "";
                    String zero = (secs < 10) ? "0" : "";
                    timeLeftLabel.setText(minus + mins + ":" + zero + secs);
                }
            });
            player.play();
            playButton.setText("Stop");
        } else if (playButton.getText().equalsIgnoreCase("Stop")) {
            player.stop();
            playButton.setText("Play");
        }
    }

    @FXML
    private void pause() {
        if (player.getStatus().equals(MediaPlayer.Status.PLAYING)) {
            player.pause();
        } else {
            player.play();
        }
    }

    @FXML
    private void search(KeyEvent ke)
    {
        if (!ke.getCode().equals(KeyCode.ENTER)) return;

        albumListView.setItems(null);
        trackListView.setItems(null);

        if (searchMode.equalsIgnoreCase("Artist")) {
            final String artist = searchField.getText();

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
            albumListTask.stateProperty().addListener(new ChangeListener<Worker.State>() {
                @Override
                public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State state2) {
                    if (state2 == Worker.State.SUCCEEDED) {
                        stateLabel.textProperty().unbind();
                        albumListView.setItems(albumListTask.getValue());
                        cache.clear();
                    }
                }
            });
            new Thread(albumListTask).start();
        } else if (searchMode.equalsIgnoreCase("Track")) {

        }
    }

    @FXML
    private void download() {
        final String artist = searchField.getText();
        List<String> chosenAlbums = albumListView.getSelectionModel().getSelectedItems();
        final Map<String, List<String>> albumTracks = new HashMap<String, List<String>>();

        if (downloadMode.equalsIgnoreCase("Tracks")) {
            if (chosenAlbums.size() > 1) {
                System.out.println("Choose only one album, please");
                return;
            } else {
                String album = chosenAlbums.get(0);
                List<String> titles = trackListView.getSelectionModel().getSelectedItems();
                albumTracks.put(album, titles);
            }
        } else if (downloadMode.equalsIgnoreCase("Albums")) {
            for (String album : chosenAlbums) {
                albumTracks.put(album, cache.get(album));
            }
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose directory to save tracks");
        if (saveDir != null) {
            chooser.setInitialDirectory(new File(saveDir));
        }
        final File selectedDirectory = chooser.showDialog(root.getScene().getWindow());
        if (selectedDirectory == null) return;
        saveDir = selectedDirectory.getAbsolutePath() + "/";

        DownloadSongsTask downloadSongsTask = new DownloadSongsTask(artist, albumTracks, saveDir);
        stateLabel.textProperty().bind(downloadSongsTask.messageProperty());
        new Thread(downloadSongsTask).start();
    }
}
