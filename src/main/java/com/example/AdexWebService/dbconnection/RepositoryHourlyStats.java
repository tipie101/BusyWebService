package com.example.AdexWebService.dbconnection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface RepositoryHourlyStats extends CrudRepository<RequestStat, Integer> {

    @Query(value = "SELECT h FROM RequestStat h WHERE h.customerId = :customer_id")
    public List<RequestStat> findRequestsForCostumer(@Param("customer_id") Long customer_id);

    @Query(value = "SELECT h FROM RequestStat h WHERE h.customerId = :customer_id AND h.time >= :min_time")
    public List<RequestStat> findRecentRequestsForCostumer(
            @Param("customer_id") Long customer_id, @Param("min_time") Long min_time);

}
