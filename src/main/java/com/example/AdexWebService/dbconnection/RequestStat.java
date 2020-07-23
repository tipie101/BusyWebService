package com.example.AdexWebService.dbconnection;

import javax.persistence.*;

@Entity
@Table(name="hourly_stats")
public class RequestStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;

    // TODO: constrain: int(11)
    @Column(name="customer_id")
    private Integer customerId;

    // TODO: constrain: timestamp
    @Column(name="time")
    private Integer time;

    // TODO: bigint(20)
    @Column(name="request_count")
    private Integer requestCount;
    @Column(name="invalid_count")
    private Integer invalidCount;

    protected RequestStat() {

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

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
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
