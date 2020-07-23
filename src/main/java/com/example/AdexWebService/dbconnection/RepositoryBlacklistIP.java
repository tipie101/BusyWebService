package com.example.AdexWebService.dbconnection;

import org.springframework.data.repository.CrudRepository;

import java.math.BigInteger;

public interface RepositoryBlacklistIP extends CrudRepository<BlacklistIP, BigInteger> {
}
