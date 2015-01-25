package com.mocsmart.musiker;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class Lastfm {

    static final String API_KEY = "dfb9264f3a637ca8c60cfe40220f15f5";
    static final String API_URL = "http://ws.audioscrobbler.com/2.0/?";
    static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder builder = null;

    static {
        try {
            Lastfm.builder = Lastfm.builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getArtistAlbums(String artist)
    {
        List<String> albums = null;
        String urlString = null;
        try {
            urlString = API_URL +
                    "method=artist.gettopalbums&" +
                    "artist=" + URLEncoder.encode(artist, "UTF-8")  +
                    "&autocorrect=1" +
                    "&api_key=" + API_KEY;

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            Document document = Lastfm.builder.parse(is);
            is.close();
            albums = albumsFromXML(document);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return albums;
    }

    public static List<String> getAlbumTracks(String artist, String album)
    {
        List<String> tracks = null;
        String urlString = null;

        try {
            urlString = API_URL +
                    "method=album.getinfo" +
                    "&artist=" + URLEncoder.encode(artist, "UTF-8") +
                    "&album=" + URLEncoder.encode(album, "UTF-8") +
                    "&autocorrect=1" +
                    "&api_key=" + API_KEY;

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            Document document = Lastfm.builder.parse(is);
            is.close();
            tracks = tracksFromXML(document);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return tracks;
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
}
