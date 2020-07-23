package com.example.AdexWebService.dbconnection;

import javax.persistence.*;

@Entity
@Table(name="ua_blacklist")
public class BlacklistUA {

    @Id
    @Column(name="ua")
    private String ua;

    public BlacklistUA() {}

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }
}
