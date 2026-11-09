package com.quantpf.finance;

import com.quantpf.domain.entity.TradingAccount;
import com.quantpf.domain.persistence.mapper.TradingAccountMapper;
import com.quantpf.domain.persistence.record.TradingAccountRow;
import com.quantpf.domain.repository.TradingAccountRepository;
import com.quantpf.domain.value.Money;
import java.util.Optional;

public class TradingAccountRepositoryAdapter implements TradingAccountRepository {
    private final TradingAccountMapper mapper;
    public TradingAccountRepositoryAdapter(TradingAccountMapper mapper){this.mapper=mapper;}
    @Override public Optional<TradingAccount> findById(Long id){TradingAccountRow row=mapper.selectById(id);if(row==null)return Optional.empty();TradingAccount account=new TradingAccount(row.getId(),row.getAccountCode(),new Money(row.getInitialCash()));account.restoreBalances(new Money(row.getAvailableCash()),new Money(row.getFrozenCash()));return Optional.of(account);}
    @Override public TradingAccount save(TradingAccount account){TradingAccountRow row=new TradingAccountRow();row.setId(account.getId());row.setAccountCode(account.getAccountCode());row.setAccountName(account.getAccountName());row.setAccountType(account.getAccountType().name());row.setCurrency("CNY");row.setInitialCash(account.getInitialCash().value());row.setAvailableCash(account.getAvailableCash().value());row.setFrozenCash(account.getFrozenCash().value());row.setStatus(account.getStatus().name());if(account.getId()==null)mapper.insert(row);else mapper.updateBalances(account.getId(),row.getAvailableCash(),row.getFrozenCash(),row.getStatus());return account;}
}