package com.yali.parking.demo;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Created by ywu on 5/28/16.
 */
public class ParkingServiceMain {


    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(
            ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new ParkingServiceSMS()), "/sms");
        context.addServlet(new ServletHolder(new ParkingServiceVoice()), "/voice");

        context.addServlet(new ServletHolder(new TwilioHandleVoiceServlet()), "/handle-voice");

        context.addServlet(new ServletHolder(new TwilioHandleRecordingServlet()), "/handle-recording");
        server.start();
        server.join();
    }
}
