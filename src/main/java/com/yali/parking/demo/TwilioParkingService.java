package com.yali.parking.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class TwilioParkingService extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String GOOGLE_GEOCODING_API_ROOT = "https://maps.googleapis.com/maps/api/geocode/xml?";
	public static final String API_KEY = "AIzaSyBUgDwlLK-SFO8QdSEoM9RTrcI0_CoJVbk";

	private HttpClientBuilder clientBuilder = HttpClientBuilder.create();

	private Log log = LogFactory.getLog(TwilioParkingService.class);
	private ParkingLocator parkingLocator = new ParkingLocator();

	ConcurrentHashMap<String, String> numberToLagLngMap = new ConcurrentHashMap<String, String>();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String fromNumber = req.getParameter("From");
		String address;
		String latlng = numberToLagLngMap.get(fromNumber);

		if (latlng == null || latlng.isEmpty()) {
			address = req.getParameter("Body");
			latlng = getLatLong(address);
			if (latlng == null) {
				getTwiMLForGatheringAddress(req, resp);
			} else {
				numberToLagLngMap.put(fromNumber, latlng);
				getTwiMLForGatheringRadius(req, resp);
			}
		} else {
			String radiusString=req.getParameter("Body");
			radiusString= (radiusString.contains("mile"))? radiusString.substring(0, radiusString.indexOf("mile")).trim() :radiusString ;
			try {
				Double radius = Double.parseDouble(radiusString);
				getTwiMLForSmsResponse(fromNumber,latlng,radius, resp);
				
			} catch (NumberFormatException nfe) {
				getTwiMLForGatheringRadius(req, resp);
			}
		} 
	}

	private void getTwiMLForSmsResponse(String fromNumber, String lagLng,
			double radius, HttpServletResponse resp) throws ServletException,
			IOException {

		// radius = Double.parseDouble(req.getParameter("Body"));
		String parkings = parkingLocator.getAvailableParking(lagLng, radius);

		if (parkings == null || parkings.isEmpty()) {
			resp.getWriter()
					.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
							+ "<Response>\n"
							+ "<Message from=\"+12403033451\"> No parking found, please increase your search radius"
							+ "</Message>\n" + "</Response>");
			resp.setContentType("application/xml");

		} else {
			resp.getWriter().print(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
							+ "<Response>\n"
							+ "<Message from=\"+12403033451\">Parking spots found:\n"
							+ parkings + "</Message>\n" + "</Response>");
			resp.setContentType("application/xml");

			numberToLagLngMap.remove(fromNumber);
		}

	}

	private void getTwiMLForGatheringAddress(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		resp.getWriter()
				.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
						+ "<Response>\n"
						+ "<Message from=\"+12403033451\">"
						+ "Welcome to ParkingMadeEasy! Please enter your address again"
						+ "</Message>\n" + "</Response>");
		resp.setContentType("application/xml");
	}

	private void getTwiMLForGatheringRadius(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		resp.getWriter()
				.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
						+ "<Response>\n"
						+ "<Message from=\"+12403033451\">"
						+ "Welcome to ParkingMadeEasy! Please narrow your search "
						+ "radius by the miles" + "</Message>\n"
						+ "</Response>");
		resp.setContentType("application/xml");
	}

	public String getLatLong(String addr) throws ClientProtocolException,
			IOException {

		log.info("address to be looked up: " + addr);
		String fullURL = GOOGLE_GEOCODING_API_ROOT + "key=" + API_KEY
				+ "&address=" + addr.replace(" ", "+");
		HttpGet getRequest = new HttpGet(fullURL);

		log.info("full URL for geocoding API: " + fullURL);

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

								if (nodes.item(k).getNodeName().equals("lat"))
									lat = nodes.item(k).getTextContent();

								if (nodes.item(k).getNodeName().equals("lng"))
									lng = nodes.item(k).getTextContent();
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

	public static void main(String[] args) throws Exception {
		Server server = new Server(Integer.valueOf(System.getenv("PORT")));
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new TwilioParkingService()), "/*");
		server.start();
		server.join();
	}
}
