package com.csee.swplus.mileage.etcSubitem.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "_sw_mileage_record")
public class EtcSubitem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String semester;

    @Column(name = "category_id")
    private int categoryId;

    @Column(name = "subitem_id")
    private int subitemId;

    private String snum;

    private String sname;

    private int value;

    @Column(name = "m_point")
    private int mPoint;

    @Column(name = "extra_point")
    private int extraPoint;

    private String description1;

    private String description2;

    private LocalDateTime moddate;

    private LocalDateTime regdate;
}
