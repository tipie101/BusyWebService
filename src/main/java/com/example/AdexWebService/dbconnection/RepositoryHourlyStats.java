package com.example.AdexWebService.dbconnection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping
@Transactional
public interface RepositoryHourlyStats extends CrudRepository<RequestStat, Integer> {

    // TODO: Iterable???
/*    @Query("SELECT h FROM RequestStat WHERE h.customer_id =: customer_id")
    Iterable<RequestStat> findRequestsForCostumer(@Param("customer_id") Integer customer_id);*/



}
