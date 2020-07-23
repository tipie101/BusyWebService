package com.example.AdexWebService.dbconnection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;

@Entity
@Table(name="ip_blacklist")
public class BlacklistIP {

    @Id
    @Column(name="ip")
    private BigInteger ip;

}
