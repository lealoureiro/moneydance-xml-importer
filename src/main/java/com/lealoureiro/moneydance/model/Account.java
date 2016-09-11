package com.lealoureiro.moneydance.model;

import java.util.UUID;

/**
 * @author Leandro Loureiro
 */
public class Account {

    private String id;
    private String name;
    private long startBalanceInCents;
    private String currency;
    private long balance;

    public Account(final String name, final long startBalanceInCents, final String currency) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.startBalanceInCents = startBalanceInCents;
        this.currency = currency;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getStartBalanceInCents() {
        return startBalanceInCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void addBalance(final long amount) {
        this.balance += amount;
    }

    public long getBalance() {
        return balance;
    }
}
