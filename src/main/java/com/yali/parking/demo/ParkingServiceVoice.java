package com.yali.parking.demo;

import com.twilio.sdk.verbs.Gather;
import com.twilio.sdk.verbs.Play;
import com.twilio.sdk.verbs.Record;
import com.twilio.sdk.verbs.Say;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

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

        // Create a TwiML response and add our friendly message.
        TwiMLResponse twiml = new TwiMLResponse();


        Gather gather = new Gather();
        gather.setAction("/handle-voice");
        gather.setNumDigits(1);
        gather.setMethod("POST");
        Say sayInGather = new Say("Need parking, please dia 1 to tell us your address!");
        try {
            gather.append(sayInGather);
            twiml.append(gather);
        } catch (TwiMLException e) {
            e.printStackTrace();
        }

        resp.setContentType("application/xml");
        resp.getWriter().print(twiml.toXML());


    }
}

class TwilioHandleVoiceServlet extends HttpServlet {

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String digits = request.getParameter("Digits");
        TwiMLResponse twiml = new TwiMLResponse();
       if (digits != null && digits.equals("1")) {
            Say pleaseLeaveMessage = new Say("Record your hawl after the tone.");
            // Record the caller's voice.
            Record record = new Record();
            record.setMaxLength(30);
            // You may need to change this to point to the location of your
            // servlet
            record.setAction("/handle-recording");
            try {
                twiml.append(pleaseLeaveMessage);
                twiml.append(record);
            } catch (TwiMLException e) {
                e.printStackTrace();
            }
        } else {
            response.sendRedirect("/twiml");
            return;
        }

        response.setContentType("application/xml");
        response.getWriter().print(twiml.toXML());
    }
}

class TwilioHandleRecordingServlet extends HttpServlet {

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String recordingUrl = request.getParameter("RecordingUrl");
        TwiMLResponse twiml = new TwiMLResponse();
        if (recordingUrl != null) {
            try {
                twiml.append(new Say("Thanks for howling... take a listen to what you howled."));
                twiml.append(new Play(recordingUrl));
                twiml.append(new Say("Goodbye"));
            } catch (TwiMLException e) {
                e.printStackTrace();
            }
        } else {
            response.sendRedirect("/twiml");
            return;
        }

        response.setContentType("application/xml");
        response.getWriter().print(twiml.toXML());
    }
}