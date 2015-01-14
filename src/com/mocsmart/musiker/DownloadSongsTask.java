package com.mocsmart.musiker;

import com.mpatric.mp3agic.*;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class DownloadSongsTask extends Task<Void> {
    private final String artist;
    private final Map<String, List<String>> albumTracks;
    private final String savePath;

    DownloadSongsTask(String artist, Map<String, List<String>> albumTracks, String savePath) {
        this.artist = artist;
        this.albumTracks = albumTracks;
        this.savePath = savePath;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("Downloading...");

        for (String album : albumTracks.keySet()) {
            createDir(savePath, album);
            List<String> tracks = albumTracks.get(album);
            for (String track : tracks) {
                String title = artist + " - " + track;
                String mp3FileName = savePath + album + "/" + title + ".mp3";
                Downloader.downloadSong(title, mp3FileName + ".tmp");
                fixMp3Tags(mp3FileName, artist, album, track);
            }
        }

        updateMessage("Downloaded");
        return null;
    }

    private static void createDir(String path, String dirName) {
        File theDir = new File(path + dirName);

        if (!theDir.exists()) {
            try{
                theDir.mkdir();
            } catch(SecurityException se){
                se.printStackTrace();
            }
        }
    }

    private static void fixMp3Tags(String mp3FileName, String artist, String album, String track) {
        String tmpFile = mp3FileName + ".tmp";
        try {
            Mp3File mp3file = new Mp3File(tmpFile);
            editID3v1tags(mp3file, artist, album, track);
            editID3v2tags(mp3file, artist, album, track);

            mp3file.save(mp3FileName);
            new File(tmpFile).delete();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedTagException e) {
            e.printStackTrace();
        } catch (InvalidDataException e) {
            e.printStackTrace();
        } catch (NotSupportedException e) {
            e.printStackTrace();
        }
    }

    private static void editID3v1tags(Mp3File mp3file, String artist, String album, String track) {
        ID3v1 id3v1Tag;
        if (mp3file.hasId3v1Tag()) {
            id3v1Tag =  mp3file.getId3v1Tag();
        } else {
            id3v1Tag = new ID3v1Tag();
            mp3file.setId3v1Tag(id3v1Tag);
        }
        id3v1Tag.setArtist(artist);
        id3v1Tag.setTitle(track);
        id3v1Tag.setAlbum(album);
    }

    private static void editID3v2tags(Mp3File mp3file, String artist, String album, String track) {
        ID3v2 id3v2Tag;
        if (mp3file.hasId3v2Tag()) {
            id3v2Tag = mp3file.getId3v2Tag();
        } else {
            id3v2Tag = new ID3v24Tag();
            mp3file.setId3v2Tag(id3v2Tag);
        }
        id3v2Tag.setArtist(artist);
        id3v2Tag.setTitle(track);
        id3v2Tag.setAlbum(album);
    }
}
