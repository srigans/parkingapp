package com.yali.parking.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

	private static final int NUMBER_OF_AVAIL_PARKING = 5;
	public static final String PARKING_AVAIL_SERVICE = "http://api.sfpark.org/sfpark/rest/availabilityservice?response=xml&uom=mile&";
	private HttpClientBuilder clientBuilder = HttpClientBuilder.create();

	private Log log = LogFactory.getLog(ParkingLocator.class);

	public static final double DEFAULT_RADIUS = 0.1;

	public String getAvailableParking(String laglng, double radius)
			throws ClientProtocolException, IOException {
		String lat = laglng.substring(0, laglng.indexOf(','));
		String lng = laglng.substring(laglng.indexOf(',')+1);

		
		if (radius<0)
			radius= DEFAULT_RADIUS;
		
		String fullURL = PARKING_AVAIL_SERVICE + "lat=" + lat + "&long=" + lng
				+ "&radius=" + radius;

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
				//responseStringBlr.append(messageNode.getTextContent()+": ");

				NodeList availNodes = doc.getElementsByTagName("AVL");
				Node availNode;
				String availName,availDesc,availInterst;
				ArrayList<ParkingAvailability> allParkings = new ArrayList<ParkingAvailability>();
				for (int i=0; (i< availNodes.getLength() && i<NUMBER_OF_AVAIL_PARKING);i++) {
					availNode=availNodes.item(i);
					ParkingAvailability  availParking = new ParkingAvailability();
					
					for (int j=0;j<availNode.getChildNodes().getLength();j++) {
						

						if (availNode.getChildNodes().item(j).getNodeName().equals("TYPE"))
						{
							availName= availNode.getChildNodes().item(j).getTextContent();
							availParking.setStatus(availName);
									
						}
						
						if (availNode.getChildNodes().item(j).getNodeName().equals("NAME"))
						{
							availName= availNode.getChildNodes().item(j).getTextContent();
							availParking.setName(availName);
									
						}
						if (availNode.getChildNodes().item(j).getNodeName().equals("DESC"))
						{
							availDesc = availNode.getChildNodes().item(j).getTextContent().replace("&", "and").replace("'", "");
							availParking.setDesc(availDesc);
									
						}
						if (availNode.getChildNodes().item(j).getNodeName().equals("INTER"))
						{
							availInterst = availNode.getChildNodes().item(j).getTextContent().replace("&", "and").replace("'", "");
							availParking.setIntersection(availInterst);
									
						}
					
					}					
					allParkings.add(availParking);
					
				}
				
				
				int count=1;
				for (ParkingAvailability p : allParkings)
				{
					responseStringBlr.append(count+". ");
					if (p.getStatus().equals("ON"))
						responseStringBlr.append(p.name);
					else
						responseStringBlr.append(p.name+" ( located on "+p.intersection+" )");
					
					responseStringBlr.append("\n");
					count++;
				}
				log.info("response:"+responseStringBlr.toString());

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
		System.out.println(test.getAvailableParking("37.7780360,-122.4325120",0.1));
	}
}

class ParkingAvailability {
	
	String status;
	String name;
	String desc;
	String intersection;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getIntersection() {
		return intersection;
	}
	public void setIntersection(String intersection) {
		this.intersection = intersection;
	}
	
}
