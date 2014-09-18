package com.yali.parking.demo;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

public class ParkingLocator {

	public static final String PARKING_AVAIL_SERVICE = "http://api.sfpark.org/sfpark/rest/availabilityservice?response=xml&uom=mile&";
	private HttpClientBuilder clientBuilder = HttpClientBuilder.create();

	private Log log = LogFactory.getLog(ParkingLocator.class);

	public static final double DEFAULT_RADIUS = 0.1;

	public String getAvailableParking(String laglng)
			throws ClientProtocolException, IOException {
		String lat = laglng.substring(0, laglng.indexOf(','));
		String lng = laglng.substring(laglng.indexOf(',')+1);

		String fullURL = PARKING_AVAIL_SERVICE + "lat=" + lat + "&long=" + lng
				+ "&radius" + DEFAULT_RADIUS;

		HttpGet getRequest = new HttpGet(fullURL);

		log.info("Full URL for SFPark availablityService API: " + fullURL);

		CloseableHttpClient httpClient = clientBuilder.build();
		HttpResponse response = httpClient.execute(getRequest);
		int statusCode = response.getStatusLine().getStatusCode();
		log.info("Calling SFPark availablityService API, status code=" + statusCode);

		Document doc = null;
		StringBuilder responseStringBlr = new StringBuilder();

		if (statusCode == 200) {

			HttpEntity entity = response.getEntity();
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();

			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				InputStream is = entity.getContent();

				doc = builder.parse(is);
				
				Node statusNode = doc.getElementsByTagName("STATUS").item(0);
				if (!statusNode.getTextContent().equals("SUCCESS")) {
					return "server error, no parking found";
				} 
				
				Node messageNode = doc.getElementsByTagName("MESSAGE").item(0);
				responseStringBlr.append(messageNode.getTextContent()+"\n");
				
				NodeList availNodes = doc.getElementsByTagName("AVL");
				for (int i=0; i< availNodes.getLength();i++) {
					
					for (int j=0;j< availNodes.item(i).getChildNodes().getLength();j++) {
						if (availNodes.item(i).getChildNodes().item(j).getNodeName().equals("NAME"))
						{
							String availName = availNodes.item(i).getChildNodes().item(j).getTextContent();
							responseStringBlr.append(availName+"\n");
									
						}
					}
				}
				return responseStringBlr.toString();
			}
			catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	
	public static void main(String args[]) throws ClientProtocolException, IOException {
		ParkingLocator test = new ParkingLocator();
		System.out.println(test.getAvailableParking("37.7780360,-122.4325120"));
	}
}
