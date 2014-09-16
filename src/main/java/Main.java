import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class Main extends HttpServlet {

	public static final String GOOGLE_GEOCODING_API_ROOT = "https://maps.googleapis.com/maps/api/geocode/json?";
	public static final String API_KEY = "AIzaSyBUgDwlLK-SFO8QdSEoM9RTrcI0_CoJVbk";

	private HttpClientBuilder clientBuilder = HttpClientBuilder.create();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		if (req.getRequestURI().endsWith("/db")) {
			showDatabase(req, resp);
		} else {
			showHome(req, resp);
		}
	}

	private void showHome(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String address = req.getHeader("Body");
		resp.getWriter()
				.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
						+ "<Response>\n"
						+ "<Message from=\"+17865162751\">Your Lat and Lng is: "
						+ getLatLong(address) + "</Message>\n" + "</Response>");
		resp.setContentType("application/xml");
	}

	public String getLatLong(String addr) throws ClientProtocolException, IOException {

		addr = addr.replace(" ", "+");
		HttpGet getRequest = new HttpGet(GOOGLE_GEOCODING_API_ROOT + "key="
				+ API_KEY + "&address=" + addr);

		CloseableHttpClient httpClient = clientBuilder.build();
		HttpResponse response = httpClient.execute(getRequest);
		int statusCode = response.getStatusLine().getStatusCode();
		String lat = null, lng=null;
		
		Document doc = null;
		if (statusCode == 200) {
			HttpEntity entity = response.getEntity();
			// String content = EntityUtils.toString(entity);

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(entity.getContent());
				NodeList nodes = doc.getChildNodes().item(0).getChildNodes();
				Node node;
				for (int i=0;i<nodes.getLength();i++) {
					node =nodes.item(i); 
					if (node.getNodeName().equals("geometry")) {
						lat = node.getChildNodes().item(0).getChildNodes().item(0).getNodeValue();
						lng =node.getChildNodes().item(0).getChildNodes().item(1).getNodeValue();
					}
				}
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}
		return lat +","+ lng;
		

	}

	private void showDatabase(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			Connection connection = getConnection();

			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
			stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
			ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

			String out = "Hello!\n";
			while (rs.next()) {
				out += "Read from DB: " + rs.getTimestamp("tick") + "\n";
			}

			resp.getWriter().print(out);
		} catch (Exception e) {
			resp.getWriter().print("There was an error: " + e.getMessage());
		}
	}

	private Connection getConnection() throws URISyntaxException, SQLException {
		URI dbUri = new URI(System.getenv("DATABASE_URL"));

		String username = dbUri.getUserInfo().split(":")[0];
		String password = dbUri.getUserInfo().split(":")[1];
		String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();

		return DriverManager.getConnection(dbUrl, username, password);
	}

	public static void main(String[] args) throws Exception {
		Server server = new Server(Integer.valueOf(System.getenv("PORT")));
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new Main()), "/*");
		server.start();
		server.join();
	}
}
