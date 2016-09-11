package com.lealoureiro.moneydance.model;

/**
 * @author Leandro Loureiro
 */
public class Category {

    private String id;
    private String name;
    private String parentId;

    public Category(final String id, final String name, final String parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }
}
