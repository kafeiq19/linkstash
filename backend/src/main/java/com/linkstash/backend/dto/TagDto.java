package com.linkstash.backend.dto;

public class TagDto {
    private Long id;
    private String name;
    private Long count;

    public TagDto() {
    }

    public TagDto(Long id, String name, Long count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public TagDto(Long id, String name) {
        this(id, name, null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
