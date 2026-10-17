package com.example.expensetracker.service;

import com.example.expensetracker.dto.AccountRequest;
import com.example.expensetracker.dto.AccountResponse;
import com.example.expensetracker.entity.Account;
import com.example.expensetracker.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return repository.findAll().stream()
                .map(AccountService::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        Account account = new Account();
        account.setName(request.name().trim());
        account.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        return toResponse(repository.save(account));
    }

    private static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getCurrency());
    }
}