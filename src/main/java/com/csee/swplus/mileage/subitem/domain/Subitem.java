package com.csee.swplus.mileage.subitem.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "_sw_mileage_subitem")
public class Subitem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Integer subitemId;
    private String subitemName;
    private Integer categoryId;
    private String categoryName;
    private String semester;
    private Boolean done;
    private String description;
}
