package com.yali.parking.demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by ywu on 5/28/16.
 */
public class ParkingServiceVoice extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private Log log = LogFactory.getLog(ParkingServiceSMS.class);
    private ParkingLocator parkingLocator = new ParkingLocator();

    ConcurrentHashMap<String, String> numberToLagLngMap = new ConcurrentHashMap<String, String>();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        resp.getWriter()
            .print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                   + "<Response>\n"
                   + "<Say voice=\"woman\"> Welcome to ParkingMadeEasy! Please send your address via text message"
                   + "</Say>\n" + "</Response>");
        resp.setContentType("application/xml");
    }

   }
