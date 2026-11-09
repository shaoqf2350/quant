package com.quantpf.domain.persistence.record;

import java.math.BigDecimal;

public class TradingAccountRow {
    private Long id; private String accountCode; private String accountName; private String accountType; private String currency; private BigDecimal initialCash; private BigDecimal availableCash; private BigDecimal frozenCash; private String status;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getAccountCode(){return accountCode;} public void setAccountCode(String v){accountCode=v;} public String getAccountName(){return accountName;} public void setAccountName(String v){accountName=v;} public String getAccountType(){return accountType;} public void setAccountType(String v){accountType=v;} public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;} public BigDecimal getInitialCash(){return initialCash;} public void setInitialCash(BigDecimal v){initialCash=v;} public BigDecimal getAvailableCash(){return availableCash;} public void setAvailableCash(BigDecimal v){availableCash=v;} public BigDecimal getFrozenCash(){return frozenCash;} public void setFrozenCash(BigDecimal v){frozenCash=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}