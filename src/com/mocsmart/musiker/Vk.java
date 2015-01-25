package com.mocsmart.musiker;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Vk {

    private static String ACCESS_TOKEN = "e571b031e3c7c6148e1acac6661cd574bba388f21452aefb93682c02af047ae05fa337c7e395f502c74fe";
    private static String API_URL = "https://api.vk.com/method/";

    public static void setAccessToken(String ACCESS_TOKEN) {
        Vk.ACCESS_TOKEN = ACCESS_TOKEN;
        //System.out.println(ACCESS_TOKEN);
    }

    private static Document songDocument(String title) {
        String formattedTitle = null;
        try {
            formattedTitle = URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String urlString = API_URL +
                "audio.search.xml?" +
                "q=" + formattedTitle +
                "&access_token=" + ACCESS_TOKEN;

        URL url = null;
        Document document = null;
        try {
            url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            document = Lastfm.builder.parse(is);
            is.close();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    private static String getFirstSongUrl(Document doc) {
        String url = null;
        XPath xPath =  XPathFactory.newInstance().newXPath();

        String expression = "/response/audio/url";
        try {
            url = xPath.compile(expression).evaluate(doc);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return url;
    }

    private static Map<String, String> getAllSongUrls(Document doc) {
        Map<String, String> res = new HashMap<>();
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/response/audio";

        try {
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node child = nodeList.item(i).getFirstChild();
                String artist = null;
                String foundTitle = null;
                String url = null;
                while (true) {
                    String nodeName = child.getNodeName();

                    if (nodeName.equals("artist")) {
                        artist = child.getFirstChild().getNodeValue();
                    } else if (nodeName.equals("title")) {
                        foundTitle = child.getFirstChild().getNodeValue();
                    } else if (nodeName.equals("url")) {
                        url = child.getFirstChild().getNodeValue();
                        break;
                    }

                    Node nextSibling = child.getNextSibling();
                    if (nextSibling != null) {
                        child = nextSibling;
                    } else {
                        break;
                    }
                }
                res.put(artist + " - " + foundTitle, url);
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static String getSongDownloadUrl(String title) {
        return getFirstSongUrl(songDocument(title));
    }

    public static Map<String, String> getAllSongUrls(String title) {
        return getAllSongUrls(songDocument(title));
    }

    private static int getFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }

    public static void downloadSong(String title, String saveAs) {
        downloadByUrl(getSongDownloadUrl(title), saveAs);
    }

    public static void downloadByUrl(String downloadUrl, String saveAs) {
        try {
            downloadUrl = downloadUrl.replace("\\", "");

            URL url = new URL(downloadUrl);

            int fileSize = getFileSize(url);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();
            OutputStream os = new FileOutputStream(saveAs);

            byte[] buffer = new byte[1024];
            int totalBytes = 0;
            int bytesRead = 0;
            //read from is to buffer
            while((bytesRead = is.read(buffer)) != -1){
                os.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                System.out.println("Downloaded " + totalBytes + " of " + fileSize + " bytes");
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
        }
    }





    public static void main(String[] args) {
        downloadSong("Scorpions - Still Loving You", "/Users/Vlad/Downloads/test.mp3");
    }


}

