package com.yali.parking.demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by ywu on 5/28/16.
 */
public class ParkingUtils {
    public static final String GOOGLE_GEOCODING_API_ROOT = "https://maps.googleapis.com/maps/api/geocode/xml?";
    public static final String API_KEY = "AIzaSyBUgDwlLK-SFO8QdSEoM9RTrcI0_CoJVbk";

    private static HttpClientBuilder clientBuilder = HttpClientBuilder.create();

    private static Log log = LogFactory.getLog(ParkingUtils.class);


    public static String getLatLong(String addr) throws ClientProtocolException,
                                                 IOException {

        log.info("Address to be looked up: " + addr);
        String fullURL = GOOGLE_GEOCODING_API_ROOT + "key=" + API_KEY
                         + "&address=" + addr.replace(" ", "+");
        HttpGet getRequest = new HttpGet(fullURL);

        log.info("Full URL for geocoding API: " + fullURL);

        CloseableHttpClient httpClient = clientBuilder.build();
        HttpResponse response = httpClient.execute(getRequest);
        int statusCode = response.getStatusLine().getStatusCode();
        log.info("Calling the Google Geocoding API, status code=" + statusCode);

        String lat = null, lng = null;
        Document doc = null;

        if (statusCode == 200) {

            HttpEntity entity = response.getEntity();
            DocumentBuilderFactory factory = DocumentBuilderFactory
                .newInstance();

            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputStream is = entity.getContent();

                doc = builder.parse(is);

                NodeList resultNode = doc.getElementsByTagName("result");

                NodeList resultChildren = resultNode.item(0).getChildNodes();
                Node resultChild;

                for (int i = 0; i < resultChildren.getLength(); i++) {
                    resultChild = resultChildren.item(i);

                    if (resultChild.getNodeName().equals("geometry")) {
                        log.info("found geometry node");
                        Node locationNode = null;
                        for (int j = 0; j < resultChild.getChildNodes()
                            .getLength(); j++) {

                            if (resultChild.getChildNodes().item(j)
                                .getNodeName().equals("location")) {
                                log.info("found location node");
                                locationNode = resultChild.getChildNodes()
                                    .item(j);
                            }
                        }

                        if (locationNode != null) {
                            log.info("num of children of location nodes: "
                                     + locationNode.getChildNodes().getLength());

                            NodeList nodes = locationNode.getChildNodes();
                            for (int k = 0; k < nodes.getLength(); k++) {

                                if (nodes.item(k).getNodeName().equals("lat")) {
                                    lat = nodes.item(k).getTextContent();
                                }

                                if (nodes.item(k).getNodeName().equals("lng")) {
                                    lng = nodes.item(k).getTextContent();
                                }
                            }
                        }
                    }
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        } else {
            log.info("Opppos, Google Geocoding API failed!");
            return null;
        }
        return lat + "," + lng;

    }

}
