package com.example.AdexWebService.dbconnection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;


public interface RepositoryHourlyStats extends CrudRepository<RequestStat, Integer> {

    @Query(value = "SELECT h FROM RequestStat h WHERE h.customerId = :customer_id")
    public List<RequestStat> findRequestsForCostumer(@Param("customer_id") Long customer_id);

    @Query(value = "SELECT h FROM RequestStat h WHERE h.customerId = :customer_id AND h.time = :hour")
    public List<RequestStat> findHourlyStatsForCostumer(
            @Param("customer_id") Long customer_id, @Param("hour") Timestamp hour);

    @Query(value = "SELECT h FROM RequestStat h WHERE DATE_FORMAT(h.time, '%Y-%m-%d') = :date")
    public List<RequestStat> findStatsOfDay(@Param("date") String date);

    @Query(value = "SELECT h FROM RequestStat h WHERE h.customerId = :customer_id AND DATE_FORMAT(h.time, '%Y-%m-%d') = :date")
    List<RequestStat> findStatsOfDayForCostumer(@Param("customer_id") Long customerID, @Param("date") String date);
}
