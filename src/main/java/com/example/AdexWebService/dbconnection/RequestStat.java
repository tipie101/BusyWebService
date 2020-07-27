package com.example.AdexWebService.dbconnection;

import javax.persistence.*;
import java.math.BigInteger;
import java.sql.Timestamp;

@Entity
@Table(name="hourly_stats")
public class RequestStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    // TODO: constrain: int(11)
    @Column(name="customer_id")
    private Long customerId;

    // TODO: constrain: timestamp
    @Column(name="time")
    private Timestamp time;

    // TODO: bigint(20)
    @Column(name="request_count")
    private Integer requestCount;
    @Column(name="invalid_count")
    private Integer invalidCount;

    public RequestStat() {

    }

    @Override
    public String toString() {
        return "Request{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", time=" + time +
                ", requestCount=" + requestCount +
                ", invalidCount=" + invalidCount +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public Integer getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount) {
        this.requestCount = requestCount;
    }

    public Integer getInvalidCount() {
        return invalidCount;
    }

    public void setInvalidCount(Integer invalidCount) {
        this.invalidCount = invalidCount;
    }
}
