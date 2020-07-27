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
import java.util.*;

// TODO:
// {"customerID":1,"tagID":2,"userID":"aaaaaaaa-bbbb-cccc-1111-222222222222","remoteIP":"123.234.56.78","timestamp":1500000000}
// write the formal validity checks
// return json response
// write db checks
// Create the tables in MYSQL
// Complete the tasks
// What about huge traffic? What about security?

// Annotation makes it that this class is automatically detected by project compilation
@RestController
public class RequestPreprocessorController {

    @Autowired
    private RepositoryHourlyStats hourlyStatRepo;
    @Autowired
    private RepositoryBlacklistIP blacklistIP;
    @Autowired
    private RepositoryBlacklistUA blacklistUA;
    @Autowired
    private RepositoryCustomer customerRepo;

    // TODO:
    // dump the db
    // create README
    // refactor and write comments + docs
    // check and implement all constraints (like int(11) etc.)


    @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/requests")
    public JSONObject handleRequest(@RequestBody String body) {
        boolean valid = true;
        JSONObject json;
        try {
            JSONParser parser = new JSONParser();
            json = (JSONObject) parser.parse(body);
        } catch (ParseException e) {
            System.out.println(e.toString());
            return new JSONObject();
        }

        // check if all relevant-fields exist:
        Long customerID = (Long) json.get("customerID");
        Long timestamp = (Long) json.get("timestamp");
        Long tagID = (Long) json.get("tagID");
        String userID = (String) json.get("userID");

        if (timestamp <= 0) {
            valid = false;
        }

        if (isBlacklisted(userID)) {
            userID = "BLACKLISTED!!!";
            valid = false;
        }

        String remoteIP = (String) json.get("remoteIP");
        // check blacklist

        if (!isValidIP(remoteIP)) {
            // add(convertIP(remoteIP)) to blacklist
            remoteIP = "invalid IP";
            valid = false;

        }
        BigInteger ipConverted = convertIP(remoteIP);

        if (isBlacklisted(ipConverted)) {
            remoteIP = "blacklisted IP";
            valid = false;
        }

        // does the (unblocked) user exist in the db
        boolean activeCustomerFound = false;
        Optional<Customer> customerReply = customerRepo.findById(customerID);
        if (customerReply.isPresent()) {
            activeCustomerFound = customerReply.get().getActive();
        }

        update_hourly_stats(customerID, timestamp, valid);
        return processValidRequest(customerID, tagID, userID, remoteIP, timestamp, activeCustomerFound);
    }

    public JSONObject processValidRequest(Long customerID, Long tagID, String userID, String remoteID, Long t, boolean active) {
        // just a stub function
        JSONObject json = new JSONObject();
        json.put("customerID", customerID);
        json.put("userID", userID);
        json.put("tagID", tagID);
        json.put("remoteID", remoteID);
        json.put("active", active);
        return json;
        // return "Successful Received: " + customerID + "(active:" + active + "), " + tagID + ", " + userID + ", " + remoteID + ", " + t;
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
        // check if customer exists in DB and date is valid
        // date has have format xxxx-xx-xx (autocreate zeros for m and d)
        List<RequestStat> stats = hourlyStatRepo.findStatsOfDayForCostumer(customerID, date);
        if (stats.isEmpty()){
            System.out.println("nothing found customer");
        } else {
            System.out.println("found customer");
        }

        int[] requestCounts  = aggregateRequestCounts(stats);
        return buildResponseJSON(requestCounts, stats);
    }

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

    @RequestMapping(method = RequestMethod.GET, path = "/dailyStats")
    public JSONObject getDailyStats(@PathParam("date") String date) {
        // TODO: Check if Date exists

        List<RequestStat> stats = hourlyStatRepo.findStatsOfDay(date);
        if (stats.isEmpty()){
            System.out.println("stats.isEmpty()");
        } else {
            System.out.println("stats is not Empty");
        }
        System.out.println(date);

        int[] requestCounts  = aggregateRequestCounts(stats);
        return buildResponseJSON(requestCounts, stats);
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

    // TODO convert ip address to int!!!
    public boolean isBlacklisted(Integer ip) {
        return false;
    }

}
