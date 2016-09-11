package com.lealoureiro.moneydance.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Leandro Loureiro
 */
public class Transaction {

    private String id;
    private String accountId;
    private String description;
    private long amount;
    private Date date;
    private String category;
    private String subCategory;
    private List<String> tags;

    public Transaction(final String accountId, final String description, final long amount, final Date date, final String category, final String subCategory) {
        this.id = UUID.randomUUID().toString();
        this.tags = new ArrayList<>();
        this.accountId = accountId;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.subCategory = subCategory;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }

    public Date getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public List<String> getTags() {
        return tags;
    }
}
