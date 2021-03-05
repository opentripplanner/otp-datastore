package controllers;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import models.fieldtrip.ScheduledFieldTrip;
import models.fieldtrip.FieldTripFeedback;
import models.fieldtrip.FieldTripRequest;
import models.fieldtrip.GroupItinerary;
import models.fieldtrip.GTFSTrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import static controllers.Application.checkLogin;
import static controllers.Calltaker.checkAccess;
import play.*;
import play.mvc.*;

import java.text.DateFormatSymbols;
import java.util.*;

import models.*;
import models.fieldtrip.FieldTripNote;
import play.data.binding.As;

import javax.ws.rs.core.MediaType;
import com.sun.jersey.api.client.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.codec.digest.DigestUtils;


public class FieldTrip extends Application {
    
    @Util
    public static void checkAccess(TrinetUser user) {
        if(user == null) {
            System.out.println("null user in FieldTrip module");
            forbidden("null user");
        }
        if(!user.hasFieldTripAccess()) {
            System.out.println("User " + user.username + " has insufficient access for FieldTrip module");
            forbidden("insufficient access privileges");
        }
    }
    
    /*@Before(unless={"newTrip","addTripFeedback","getCalendar","newRequest","newRequestForm"}, priority=1)
    public static void checkLogin () {
        String username = params.get("userName");
        String password = params.get("password");
        
        System.out.println("checkLogin "+username);
        User user = checkUser(username, password);
        //User user = getUser();
        if (user == null || !user.canScheduleFieldTrips()) {
            forbidden();
        }
        System.out.println("checkLogin success");
    }*/

    public static void index() {
        //index at present does nothing
        render();
    }

    /**
     * y/m/d are the day for which we would like a calendar.
     */
    public static void getCalendar(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        if(year == 0) year = cal.get(Calendar.YEAR);
        if(month == 0) month = cal.get(Calendar.MONTH)+1;
        if(day == 0) day = cal.get(Calendar.DAY_OF_MONTH);

        List<ScheduledFieldTrip> fieldTrips;

        fieldTrips = ScheduledFieldTrip.find("year(serviceDay) = ? and month(serviceDay) = ? and day(serviceDay) = ? " +
                                              "order by departure", 
                                              year, month, day).fetch();

        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        String monthName = months[month - 1];
        render(fieldTrips, year, month, monthName, day);
    }

    public static void opsReport(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        if(year == 0) year = cal.get(Calendar.YEAR);
        if(month == 0) month = cal.get(Calendar.MONTH)+1;
        if(day == 0) day = cal.get(Calendar.DAY_OF_MONTH);

        List<GTFSTrip> gtfsTrips = GTFSTrip.find("year(groupItinerary.fieldTrip.serviceDay) = ? and month(groupItinerary.fieldTrip.serviceDay) = ? and day(groupItinerary.fieldTrip.serviceDay) = ? " +
                                              "order by depart", 
                                              year, month, day).fetch();

        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        String monthName = months[month - 1];
        render(gtfsTrips, year, month, monthName, day);
    }
    
    
    public static void getFieldTrip(long id) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(id);
        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(fieldTrip));
    }
    
    public static void getFieldTrips(@As("MM/dd/yyyy") Date date, Integer limit) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        System.out.println("getFTs, date="+date);
        List<ScheduledFieldTrip> trips;
        String sql = "";
        if(date != null) {          
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            sql = "year(serviceDay) = " + cal.get(Calendar.YEAR) + 
                  " and month(serviceDay) = " + (cal.get(Calendar.MONTH)+1) + 
                  " and day(serviceDay) = "+cal.get(Calendar.DAY_OF_MONTH)+" ";
        }
        sql += "order by departure";
        if(limit == null)
            trips = ScheduledFieldTrip.find(sql).fetch();
        else {
            trips = ScheduledFieldTrip.find(sql).fetch(limit);
        }

        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(trips));
        //renderJSON(trips);
    }
    
    public static void getGTFSTripsInUse(@As("MM/dd/yyyy") Date date, Integer limit) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        System.out.println("getFTs, date="+date);
        List<ScheduledFieldTrip> trips;
        String sql = "";
        if(date != null) {          
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            sql = "year(serviceDay) = " + cal.get(Calendar.YEAR) + 
                  " and month(serviceDay) = " + (cal.get(Calendar.MONTH)+1) + 
                  " and day(serviceDay) = "+cal.get(Calendar.DAY_OF_MONTH)+" ";
        }
        sql += "order by departure";
        if(limit == null)
            trips = ScheduledFieldTrip.find(sql).fetch();
        else {
            trips = ScheduledFieldTrip.find(sql).fetch(limit);
        }

        Set<GTFSTrip> gtfsTrips = new HashSet<GTFSTrip>();
        for(ScheduledFieldTrip fieldTrip : trips) {
            for(GroupItinerary itin : fieldTrip.groupItineraries) {
                for(GTFSTrip gtfsTrip : itin.trips) {
                    gtfsTrips.add(gtfsTrip);
                }
            }
        }
        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(gtfsTrips));
    }

    public static void newTrip(long requestId, ScheduledFieldTrip trip, GroupItinerary[] itins, GTFSTrip[][] gtfsTrips) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
        
        FieldTripRequest ftRequest = FieldTripRequest.findById(requestId);

        // first, check that trip is possible
        for(int i = 0; i < itins.length; i++) {
            GroupItinerary itin = itins[i];
            for(GTFSTrip gtrip : gtfsTrips[i]) {
                List<GTFSTrip> tripsInUse = GTFSTrip.find("tripHash = ?", gtrip.tripHash).fetch();
                
                if(!tripsInUse.isEmpty()) {
                    int capacityInUse = 0;
                    for(GTFSTrip tripInUse : tripsInUse) {
                        
                        // are dates the same?
                        if(!tripInUse.groupItinerary.fieldTrip.request.travelDate.equals(ftRequest.travelDate)) continue;
                            
                        // do the stop ranges overlap?
                        if(gtrip.fromStopIndex >= tripInUse.toStopIndex || gtrip.toStopIndex <= tripInUse.fromStopIndex) continue;
                        
                        capacityInUse += tripInUse.groupItinerary.passengers;
                    }
                    int remainingCapacity = gtrip.capacity - capacityInUse;
                    if(itin.passengers > remainingCapacity) {
                        renderJSON(-1);
                    }
                }
            }
        }

        
        // delete any existing ScheduledFieldTrip(s) at this requestOrder index 
        
        Set<ScheduledFieldTrip> tripsToDelete = new HashSet<ScheduledFieldTrip>();
        for(ScheduledFieldTrip reqTrip : ftRequest.trips) {
            if(reqTrip.requestOrder == trip.requestOrder) tripsToDelete.add(reqTrip);
        }            
        for(ScheduledFieldTrip delTrip : tripsToDelete) {
            delTrip.delete();
            ftRequest.trips.remove(delTrip);
        }


        // save the new ScheduledFieldTrip
        
        //TODO: is setting id to null the right way to ensure that an
        //existing trip is not overwritten?
        trip.id = null;
        trip.request = ftRequest;
        trip.serviceDay = trip.departure;
        trip.createdBy = user.username;
        trip.save();
        
        // add the ScheduledFieldTrip to the request
    
        ftRequest.trips.add(trip);
        ftRequest.updateTripStatusFields();
        ftRequest.save();
        
        // create the GroupItineraries and GTFSTrips
        trip.groupItineraries = new ArrayList<GroupItinerary>();
        for(int i = 0; i < itins.length; i++) {
            GroupItinerary itinerary = itins[i];
            itinerary.fieldTrip = trip;
            trip.groupItineraries.add(itinerary);
            itinerary.save();
            
            itinerary.trips = new ArrayList<GTFSTrip>();
            for(GTFSTrip gtrip : gtfsTrips[i]) {
                gtrip.groupItinerary = itinerary;
                itinerary.trips.add(gtrip);
                gtrip.save();
            }
        }
        

        Long id = trip.id;
        renderJSON(id);
    }
    
    public static void addItinerary(long fieldTripId, GroupItinerary itinerary, GTFSTrip[] trips) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        System.out.println("aI / fieldTripId="+fieldTripId);
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(fieldTripId);
        //System.out.println("aI / fieldTrip="+fieldTrip);
        itinerary.fieldTrip = fieldTrip;
        fieldTrip.groupItineraries.add(itinerary);
        itinerary.save();
        Long id = itinerary.id;
        //GroupItinerary itin2 = GroupItinerary.findById(id);
        
        itinerary.trips = new ArrayList<GTFSTrip>();
        for(GTFSTrip gtrip : trips) {
          gtrip.groupItinerary = itinerary;
          itinerary.trips.add(gtrip);
          gtrip.save();
        }
        renderJSON(id);
    }

    public static void deleteTrip(Long id) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        ScheduledFieldTrip trip = ScheduledFieldTrip.findById(id);
        trip.delete();
        renderJSON(id);
    }
    
    /* FieldTripRequest methods */
    
    public static void newRequestForm() {
        render();
    }
    
    public static void newRequest(FieldTripRequest req, String recaptcha_challenge_field, String recaptcha_response_field) {
        
        // check captcha
        String publicKey = (String) Play.configuration.get("recaptcha.public_key");
        String privateKey = (String) Play.configuration.get("recaptcha.private_key");

        String gRecaptchaResponse = params.get("g-recaptcha-response");

        try {
            URL url = new URL("https://www.google.com/recaptcha/api/siteverify");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            // add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String postParams = "secret=" + privateKey + "&response="
                    + gRecaptchaResponse;

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postParams);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer recaptchaResponse = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                recaptchaResponse.append(inputLine);
            }
            in.close();

            JsonReader jsonReader = new JsonReader(new StringReader(recaptchaResponse.toString()));
            jsonReader.beginObject();
            boolean recaptchaSuccess = false;
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if(name.equals("success")) {
                    recaptchaSuccess = jsonReader.nextBoolean();
                }
                else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            jsonReader.close();

            String errCode;

            if (!recaptchaSuccess) {
                errCode = "err_recaptcha";
            }
            else if(req.teacherName == null || req.teacherName.length() == 0) {
                errCode = "err_teachername";
            }
            else if(!checkDate(req.travelDate)) {
                errCode = "err_traveldate";
            }
            else {
                req.id = null;
                req.save();
                errCode = "ok";
            }

            render(req, errCode);
        }
        catch(Exception e) {
            e.printStackTrace();
            badRequest();
        }
    }
    
    @Util
    protected static boolean checkDate(Date date) {
        if(date == null) return false;
        
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(Calendar.DATE, 1);
        
        Calendar now = Calendar.getInstance();
        
        //if(cal.after(now)) return true;
        
        return true;	
    }

    public static void getRequest(long requestId) {
        TrinetUser user = checkLogin();        
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);

        System.out.println("OTS = " + req.outboundTripStatus);
        if(req != null) {
            Gson gson = new GsonBuilder()
              .excludeFieldsWithoutExposeAnnotation()  
              .serializeNulls()
              .create();
            renderJSON(gson.toJson(req));
        }
        else {
            badRequest();
        }   
    }

    public static void getRequests(Integer limit) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        List<FieldTripRequest> requests;
        String sql = "order by timeStamp desc";
        if(limit == null)
            requests = FieldTripRequest.find(sql).fetch();
        else {
            requests = FieldTripRequest.find(sql).fetch(limit);
        }

        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .serializeNulls()
          .create();
        renderJSON(gson.toJson(requests));
    }

    public static void getRequestsSummary(Integer limit) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        List<FieldTripRequest> requests;
        String sql = "order by timeStamp desc";
        if(limit == null)
            requests = FieldTripRequest.find(sql).fetch();
        else {
            requests = FieldTripRequest.find(sql).fetch(limit);
        }

        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()
          .setExclusionStrategies(new ExclusionStrategy() {

            public boolean shouldSkipField(FieldAttributes fa) {
                String name = fa.getName();
                return(name.equals("trips") || name.equals("notes") || name.equals("feedback"));
            }

            public boolean shouldSkipClass(Class<?> type) {
                return false;
            }
              
          })
          .serializeNulls()
          .create();
        renderJSON(gson.toJson(requests));
    }
    
    public static void setRequestStatus(long requestId, String status) {
        TrinetUser user = checkLogin();        
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            req.status = status;
            req.save();
            if(status.equals("cancelled")) {
                for(ScheduledFieldTrip trip : req.trips) {
                    trip.delete();
                }
            }
            renderJSON(requestId);
        }
        else {
            badRequest();
        }        
    }

    public static void setRequestClasspassId(long requestId, String classpassId) {
        TrinetUser user = checkLogin();        
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            if(classpassId != null && classpassId.length() == 0) classpassId = null;
            req.classpassId = classpassId;
            req.save();
            renderJSON(requestId);
        }
        else {
            badRequest();
        }        
    }

    public static void setRequestPaymentInfo(long requestId, String classpassId, String paymentPreference, String ccType, String ccName, String ccLastFour, String checkNumber) {
        TrinetUser user = checkLogin();
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            if(classpassId != null && classpassId.length() == 0) classpassId = null;
            req.classpassId = classpassId;

            req.paymentPreference = paymentPreference;

            if(ccType != null && ccType.length() == 0) ccType = null;
            req.ccType = ccType;

            if(ccName != null && ccName.length() == 0) ccName = null;
            req.ccName = ccName;

            if(ccLastFour != null && ccLastFour.length() == 0) ccLastFour = null;
            req.ccLastFour = ccLastFour;

            if(checkNumber != null && checkNumber.length() == 0) checkNumber = null;
            req.checkNumber = checkNumber;

            req.save();
            renderJSON(requestId);
        }
        else {
            badRequest();
        }
    }

    public static void setRequestDate(long requestId, @As("MM/dd/yyyy") Date date) {
        TrinetUser user = checkLogin();        
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            req.travelDate = date;
            req.save();
            for(ScheduledFieldTrip trip : req.trips) {
                trip.delete();
            }
            renderJSON(requestId);
        }
        else {
            badRequest();
        }        
    }

    public static void setRequestGroupSize(long requestId, int numStudents, int numFreeStudents, int numChaperones) {
        TrinetUser user = checkLogin();
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            req.numStudents = numStudents;
            req.numFreeStudents = numFreeStudents;
            req.numChaperones = numChaperones;
            req.save();
            renderJSON(requestId);
        }
        else {
            badRequest();
        }
    }

    public static void updateRequests() {
        List<FieldTripRequest> requests = FieldTripRequest.find("").fetch();
        for(FieldTripRequest req : requests) {
            req.updateTripStatusFields();
            req.save();
        }
        String status = "Finished updating requests.";
        render(status);
    }
    
    /* FieldTripFeedback */
    
    public static void feedbackForm(long requestId) {
        System.out.println("ff req="+requestId);
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            render(req);
        }
        else {
            badRequest();
        }
    }
    
    public static void addFeedback(FieldTripFeedback feedback, long requestId) {
        System.out.println("addFeedback reqId="+requestId);
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            feedback.id = null;
            feedback.request = req;
            feedback.save();
            
            req.feedback.add(feedback);
            req.save();
            
            render(feedback);
        }
        else {
            badRequest();
        }
    }

    public static void addNote(FieldTripNote note, long requestId) {
        TrinetUser user = checkLogin();        
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            note.id = null;
            note.request = req;
            note.save();
            
            req.notes.add(note);
            req.save();
            
            renderJSON(requestId);
        }
        else {
            badRequest();
        }
    }
    
    public static void deleteNote(Long noteId) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        FieldTripNote note = FieldTripNote.findById(noteId);
        note.delete();
        renderJSON(noteId);
    }

    public static void editSubmitterNotes(String notes, long requestId) {
        TrinetUser user = checkLogin();        
        checkAccess(user);

        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            req.submitterNotes = notes;
            req.save();
            
            renderJSON(requestId);
        }
        else {
            badRequest();
        }
    }
    
    public static void searchRequests(String query, String teacherValue, String schoolValue, @As("MM/dd/yyyy") Date date1, @As("MM/dd/yyyy") Date date2) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        System.out.println("search: "+query);
        List<FieldTripRequest> requests;
        if(date2 == null) requests = FieldTripRequest.find(query, teacherValue, schoolValue, date1).fetch();
        else requests = FieldTripRequest.find(query, teacherValue, schoolValue, date1, date2).fetch();

        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .serializeNulls()
          .create();
        renderJSON(gson.toJson(requests));
    }
        
    /* Receipt Generation */
    
    public static void receipt(long requestId) {
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            render(req);
        }
    }

}