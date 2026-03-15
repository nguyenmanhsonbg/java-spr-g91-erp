package com.g90.backend.modules.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "ProjectWarehouseEntity")
@Table(name = "warehouses")
public class WarehouseEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "location", length = 500)
    private String location;
}
