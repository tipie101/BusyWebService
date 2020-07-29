package com.example.AdexWebService;

import com.example.AdexWebService.dbconnection.*;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.server.PathParam;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class RequestPreprocessor {

    @Autowired
    private RepositoryHourlyStats hourlyStatRepo;
    @Autowired
    private RepositoryBlacklistIP blacklistIP;
    @Autowired
    private RepositoryBlacklistUA blacklistUA;
    @Autowired
    private RepositoryCustomer customerRepo;


    @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/requests")
    public JSONObject handleRequest(@RequestBody String body) {
        JSONObject json;
        Long customerID;
        String remoteIP;
        Long timestamp;
        Long tagID;
        String userID;

        try {
            JSONParser parser = new JSONParser();
            json = (JSONObject) parser.parse(body);
        } catch (ParseException e) {
            // ill-formatted
            return rejectRequest("Badly Formatted JSON");
        }

        // check if all relevant-fields exist and filled with valid values
        try {
            customerID = (Long) json.get("customerID");
            timestamp = (Long) json.get("timestamp");
        } catch (ClassCastException e) {
            return rejectRequest("Invalid Argument");
        } catch (IllegalArgumentException e) {
            return rejectRequest("Missing an Argument");
        }
        if (customerID < 0) {
            return rejectRequest("CustomerIDs can not be negative");
        }
        if (timestamp < 0) {
            return rejectRequest("Timestamps can not be negative");
        }
        // does the (unblocked) user exist in the db
        Optional<Customer> customerReply = customerRepo.findById(customerID);
        if (customerReply.isEmpty() || !customerReply.get().getActive()) {
            return rejectRequest("No active customer found");
        }

        // from here on, stats can be updated since customerID and timestamp are valid
        try {
            tagID = (Long) json.get("tagID");
            userID = (String) json.get("userID");
            remoteIP = (String) json.get("remoteIP");
        } catch (ClassCastException e) {
            update_hourly_stats(customerID, timestamp, false);
            return rejectRequest("Invalid Argument");
        } catch (IllegalArgumentException e) {
            update_hourly_stats(customerID, timestamp, false);
            return rejectRequest("Missing an Argument");
        }

        if (!isValidIP(remoteIP)) {
            update_hourly_stats(customerID, timestamp, false);
            return rejectRequest("invalid IP");
        }
        BigInteger ipConverted = convertIP(remoteIP);

        if (isBlacklisted(userID) || isBlacklisted(ipConverted)) {
            update_hourly_stats(customerID, timestamp, false);
            return rejectRequest("Blacklisted");
        }


        update_hourly_stats(customerID, timestamp, true);
        return processValidRequest(customerID, tagID, userID, remoteIP, timestamp);
    }

    private JSONObject rejectRequest(String message) {
        JSONObject response = new JSONObject();
        response.put("Status", "Error");
        response.put("Error", message);
        return response;
    }

    public JSONObject processValidRequest(Long customerID, Long tagID, String userID, String remoteID, Long t) {
        // just a stub function
        JSONObject json = new JSONObject();
        json.put("customerID", customerID);
        json.put("userID", userID);
        json.put("tagID", tagID);
        json.put("remoteID", remoteID);
        return json;
    }

    public void update_hourly_stats(long customerID, long time, boolean validRequest) {
        // time is passed as sec
        long TimeFloor = time - (time % 3600);
        Timestamp hour = new Timestamp(TimeFloor * 1000);
        List<RequestStat> stats = hourlyStatRepo.findHourlyStatsForCostumer(customerID, hour);
        if (stats.isEmpty()) {
            System.out.println("ALL EMPTY!");
            RequestStat stat = new RequestStat();
            stat.setCustomerId(customerID);
            stat.setTime(hour);
            stat.setInvalidCount(0);
            stat.setRequestCount(1);
            if (!validRequest) {
                stat.setInvalidCount(1);
            }
            hourlyStatRepo.save(stat);
            return;
        }
            // Assert: stats.size() == 1
            // There should only be one stat per hour
            RequestStat stat = stats.get(0);
            stat.setRequestCount(stat.getRequestCount() + 1);
            if (!validRequest) {
                stat.setInvalidCount(stat.getInvalidCount() + 1);
            }
            hourlyStatRepo.save(stat);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/customersDailyStats")
    public JSONObject getCostumersDailyStats(@PathParam("customerID") Long customerID, @PathParam("date") String date) {
        // Date has to have format yyyy-mm-dd
        List<RequestStat> stats = hourlyStatRepo.findStatsOfDayForCostumer(customerID, date);
        int[] requestCounts  = aggregateRequestCounts(stats);
        return buildResponseJSON(requestCounts, stats);
    }

    // optional, wasn't needed for the coding task
    @RequestMapping(method = RequestMethod.GET, path = "/customerStats")
    public JSONObject getCostumerStats(@PathParam("customerID") Long customerID) {

        // check if customer exists in DB

        List<RequestStat> stats = hourlyStatRepo.findRequestsForCostumer(customerID);
        if (stats.isEmpty()){
            System.out.println("nothing found customer");
        } else {
            System.out.println("found customer");
        }

        int[] requestCounts  = aggregateRequestCounts(stats);

        JSONObject response = buildResponseJSON(requestCounts, stats);
        return response;
        //return "stats for customer_id " + customerID + ": #req = " + requestCounts[0] + " and #invalid = " + requestCounts[1];
    }

    // optional, wasn't needed for the coding task
    @RequestMapping(method = RequestMethod.GET, path = "/dailyStats")
    public JSONObject getDailyStats(@PathParam("date") String date) {
        // Check if date is valid
        String formatString = "yyyy-MM-dd";
        try {
            SimpleDateFormat format = new SimpleDateFormat(formatString);
            format.setLenient(false);
            format.parse(date);
        } catch (java.text.ParseException e) {
            return rejectRequest("Couldn't parse date; use yyyy-mm-dd as format");
        }

        List<RequestStat> stats = hourlyStatRepo.findStatsOfDay(date);
        int[] requestCounts  = aggregateRequestCounts(stats);
        return buildResponseJSON(requestCounts, stats);
    }

    private JSONObject buildResponseJSON(int[] requestCounts, List<RequestStat> stats) {
        JSONObject response = new JSONObject();
        response.put("all_requests_count", requestCounts[0]);
        response.put("invalid_requests_count", requestCounts[1]);
        JSONArray array = new JSONArray();
        for (RequestStat stat:  stats){
            JSONObject statJSON = new JSONObject();
            statJSON.put("customerID", stat.getCustomerId());
            statJSON.put("time", stat.getTime());
            statJSON.put("request_count", stat.getRequestCount());
            statJSON.put("invalid_request_count", stat.getInvalidCount());
            array.add(statJSON);
        }
        response.put("hourly_stats", array);
        return response;
    }

    private int[] aggregateRequestCounts(List<RequestStat> stats) {
        int requests = 0;
        int invalid = 0;
        for (RequestStat stat: stats) {
            requests += stat.getRequestCount();
            invalid += stat.getInvalidCount();
        }
        return new int[]{requests, invalid};
    }

    public boolean isValidIP(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            System.out.println("Successfully checked InternetAddress: " + inetAddress.toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public BigInteger convertIP(String ip) {
        // src: https://seancfoley.github.io/IPAddress/
        IPAddress ipAddress = new IPAddressString(ip).getAddress();
        return ipAddress.getValue();
    }

    public boolean isBlacklisted(String userAgent) {
        Optional<BlacklistUA> ua = blacklistUA.findById(userAgent);
        return ua.isPresent();
    }

    public boolean isBlacklisted(BigInteger ip) {
        Optional<BlacklistIP> ipListed = blacklistIP.findById(ip);
        return ipListed.isPresent();
    }

}
