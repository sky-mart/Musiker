package com.mocsmart.musiker;

import javafx.concurrent.Task;

import java.util.List;

public class SimpleDownloadTask extends Task<Void> {

    private List<String> titles;
    private List<String> urls;
    private String savePath;
    private int songCount;

    SimpleDownloadTask(List<String> titles, List<String> urls, String savePath) {
        this.titles = titles;
        this.urls = urls;
        this.savePath = savePath;
        songCount = titles.size();
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("Download started");

        int downloadCount = 0;
        for (int i = 0; i < songCount; i++) {
            String title = titles.get(i);
            updateMessage("Downloading " + title + " (total " + downloadCount + "/" + songCount + ")");
            String mp3FileName = savePath + "/" + title + ".mp3";
            Vk.downloadByUrl(urls.get(i), mp3FileName);
            downloadCount++;
        }
        updateMessage("Download finished");
        return null;
    }
}
