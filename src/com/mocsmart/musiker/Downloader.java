package com.mocsmart.musiker;

import com.mpatric.mp3agic.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Downloader {

    private static Ui ui;
    private static String vkAccessToken;
    private static String vkApiUrl = "https://api.vk.com/method/";
    private static final String lastfmApiKey = "dfb9264f3a637ca8c60cfe40220f15f5";
    private static final String lastfmApiUrl = "http://ws.audioscrobbler.com/2.0/?";

    private static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder builder = null;

    static {
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void setUi(Ui ui) {
        Downloader.ui = ui;
    }

    public static void setVkAccessToken(String vkAccessToken) {
        Downloader.vkAccessToken = vkAccessToken;
        //System.out.println(vkAccessToken);
    }

    public static List<String> getAlbums(String artist)
    {
        List<String> albums = null;

        artist = replacementForUrl(artist);
        String urlString = lastfmApiUrl +
                "method=artist.gettopalbums&" +
                "artist=" + artist +
                "&api_key=" +lastfmApiKey;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            Document document = builder.parse(is);
            is.close();
            albums = albumsFromXML(document);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return albums;
    }

    private static List<String> albumsFromXML(Document xmlDocument)
    {
        List<String> albums = new ArrayList<String>();
        XPath xPath =  XPathFactory.newInstance().newXPath();

        String expression = "/lfm/topalbums/album/name";

        try {
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                albums.add(nodeList.item(i).getFirstChild().getNodeValue());
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return albums;
    }

    public static List<String> getTracks(String artist, String album)
    {
        List<String> tracks = null;

        artist = replacementForUrl(artist);
        album = replacementForUrl(album);
        String urlString = lastfmApiUrl +
                "method=album.getinfo&" +
                "artist=" + artist +
                "&album=" + album +
                "&api_key=" +lastfmApiKey;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            Document document = builder.parse(is);
            is.close();
            tracks = tracksFromXML(document);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return tracks;
    }

    private static List<String> tracksFromXML(Document xmlDocument)
    {
        List<String> tracks = new ArrayList<String>();
        XPath xPath =  XPathFactory.newInstance().newXPath();

        String expression = "/lfm/album/tracks/track/name";

        try {
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                tracks.add(nodeList.item(i).getFirstChild().getNodeValue());
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return tracks;
    }

    private static String replacementForUrl(String urlPart)
    {
        return urlPart.replace(" ", "%20");
    }

    public static void downloadSongs(String artist, Map<String, List<String>> albumTracks, String savePath)
    {
        for (String album : albumTracks.keySet()) {
            List<String> tracks = albumTracks.get(album);
            for (String track : tracks) {
                String title = artist + " - " + track;
                String mp3FileName = savePath + title + ".mp3";
                downloadSong(title, mp3FileName + ".tmp");
                fixMp3Tags(mp3FileName, artist, album, track);
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

    public static void downloadSong(String title, String saveAs)
    {
        String formattedTitle = replacementForUrl(title);
        String urlString = vkApiUrl +
                "audio.search.xml?" +
                "q=" + formattedTitle +
                "&access_token=" + vkAccessToken;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            Document document = builder.parse(is);
            is.close();
            String downloadUrl = urlFromXML(document);

            downloadUrl = downloadUrl.replace("\\", "");

            url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            is = connection.getInputStream();
            OutputStream os = new FileOutputStream(saveAs);

            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from is to buffer
            while((bytesRead = is.read(buffer)) != -1){
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            //flush OutputStream to write any buffered data to file
            os.flush();
            os.close();

            //System.out.println("Song downloaded: " + savePath + title + ".mp3");
            
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private static String urlFromXML(Document xmlDocument)
    {
        String url = null;
        XPath xPath =  XPathFactory.newInstance().newXPath();

        String expression = "/response/audio/url";

        try {
            url = xPath.compile(expression).evaluate(xmlDocument);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static void main(String[] args)
    {
//        String artist = "Scorpions";
//        for (String album : getAlbums(artist)) {
//            System.out.println(album);
//            for (String track : getTracks(artist, album)) {
//                System.out.println("\t" + track);
//            }
//        }
        downloadSong("Scorpions - Still Loving You", "/User/Vlad/");
    }
}

