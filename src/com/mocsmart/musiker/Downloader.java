package com.mocsmart.musiker;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

import java.util.List;

public class Downloader implements Runnable {

    private StringProperty currentDownload;
    private IntegerProperty currentDownloadNo;
    private IntegerProperty downloadsCount;
    private DoubleProperty currentDownloadProgress;

    private List<Song> songs;

    public Downloader(List<Song> songs) {
        this.songs = songs;
    }

    public void run() {
        for (Song song : songs) {

        }
    }

    public void pauseCurrentDownload() {

    }

    public void cancelCurrentDownload() {

    }
}