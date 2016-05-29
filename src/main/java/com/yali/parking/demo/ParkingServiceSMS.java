package com.yali.parking.demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ParkingServiceSMS extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Log log = LogFactory.getLog(ParkingServiceSMS.class);
    private ParkingLocator parkingLocator = new ParkingLocator();

    ConcurrentHashMap<String, String> numberToLagLngMap = new ConcurrentHashMap<String, String>();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        String fromNumber = req.getParameter("From");
        String twilioNumber = req.getParameter("To");
        log.info("App received request parameters from Twilio : FromNumber ={" + fromNumber + "}, ToTwilioNumber={"
                 + twilioNumber + "}");
        String address;
        String latlng = numberToLagLngMap.get(fromNumber);

        if (latlng == null || latlng.isEmpty()) {
            address = req.getParameter("Body");
            latlng = ParkingUtils.getLatLong(address);
            if (latlng == null) {
                getTwiMLForGatheringAddress(req, resp, twilioNumber);
            } else {
                numberToLagLngMap.put(fromNumber, latlng);
                getTwiMLForGatheringRadius(req, resp, twilioNumber);
            }
        } else {
            String radiusString = req.getParameter("Body");
            radiusString =
                (radiusString.contains("mile")) ? radiusString.substring(0, radiusString.indexOf("mile")).trim()
                                                : radiusString;
            try {
                Double radius = Double.parseDouble(radiusString);
                getTwiMLForSmsResponse(fromNumber, twilioNumber, latlng, radius, resp);

            } catch (NumberFormatException nfe) {
                getTwiMLForGatheringRadius(req, resp, twilioNumber);
            }
        }
    }

    private void getTwiMLForSmsResponse(String fromNumber, String twilioNumber, String lagLng,
                                        double radius, HttpServletResponse resp) throws ServletException,
                                                                                        IOException {

        // radius = Double.parseDouble(req.getParameter("Body"));
        String parkings = parkingLocator.getAvailableParking(lagLng, radius);

        if (parkings == null || parkings.isEmpty()) {
            resp.getWriter()
                .print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<Response>\n"
                       + "<Message from=\"" + twilioNumber + "\"> No parking found, please increase your search radius"
                       + "</Message>\n" + "</Response>");
            resp.setContentType("application/xml");

        } else {
            resp.getWriter().print(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Response>\n"
                + "<Message from=\"" + twilioNumber + "\">"
                + parkings + "</Message>\n" + "</Response>"
            );
            resp.setContentType("application/xml");

            numberToLagLngMap.remove(fromNumber);
        }

    }

    private void getTwiMLForGatheringAddress(HttpServletRequest req,
                                             HttpServletResponse resp, String twilioNumber) throws IOException {
        resp.getWriter()
            .print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                   + "<Response>\n"
                   + "<Message from=\"" + twilioNumber + "\">"
                   + "Welcome to ParkingMadeEasy! Please enter your address again"
                   + "</Message>\n" + "</Response>");
        resp.setContentType("application/xml");
    }

    private void getTwiMLForGatheringRadius(HttpServletRequest req,
                                            HttpServletResponse resp, String twilioNumber) throws IOException {
        resp.getWriter()
            .print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                   + "<Response>\n"
                   + "<Message from=\"" + twilioNumber + "\">"
                   + "Welcome to ParkingMadeEasy! Please narrow your search "
                   + "radius by the miles, you can reply with 0.5 miles, or just 0.5" + "</Message>\n"
                   + "</Response>");
        resp.setContentType("application/xml");
    }


    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(
            ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new ParkingServiceSMS()), "/sms");
        context.addServlet(new ServletHolder(new ParkingServiceVoice()), "/voice");
        server.start();
        server.join();
    }
}
