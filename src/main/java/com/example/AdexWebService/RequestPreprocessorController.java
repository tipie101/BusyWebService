package com.example.AdexWebService;

import com.example.AdexWebService.dbconnection.*;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.apache.coyote.Request;
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
import java.util.List;
import java.util.Optional;

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
    // endpoint for creating/updating RequestStat
    // endpoints for reading RequestStats

    // invalid request -> return json:
    // {
    //   "timestamp": "2020-07-22T16:38:20.728+00:00",
    //   "status": 400,
    //   "error": "Bad Request",
    //   "message": "invalid customerID value",
    //   "path": "/requests"
    // }

    @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/requests")
    public String handleRequest(@RequestBody String body) {
        boolean valid = true;
        JSONObject json;
        try {
            JSONParser parser = new JSONParser();
            json = (JSONObject) parser.parse(body);
        } catch (ParseException e) {
            return e.toString();
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


/*        customerReply.ifPresent( x -> {
            System.out.println("hello: " + x.toString());
            System.out.println(customerReply.get().getActive());
            customer = customerReply.get();
        });*/

        // TODO: Get this to work, baby!!!
        update_hourly_stats(customerID, timestamp, valid);
        return processValidRequest(customerID, tagID, userID, remoteIP, timestamp, activeCustomerFound);
    }

    public String processValidRequest(Long customerID, Long tagID, String userID, String remoteID, Long t, boolean active) {
        // just a stub function
        return "Successful Received: " + customerID + "(active:" + active + "), " + tagID + ", " + userID + ", " + remoteID + ", " + t;
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

    @RequestMapping(method = RequestMethod.GET, path = "/customerStats")
    public String getCostumerStats(@PathParam("customerID") Integer customerID) {
        // call aggregateForCustomer()
        // call aggregateAll()
        return "stats";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/dailyStats")
    public String getDailyStats(@PathParam("date") String date) {
        // convert date
        // call aggregateForDate()
        // call aggregateAll()
        return "stats";
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
