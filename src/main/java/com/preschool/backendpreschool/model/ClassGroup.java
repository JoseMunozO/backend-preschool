package com.preschool.backendpreschool.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    private String name;

    @Column(name = "school_year")
    private String schoolYear;

    @Column(name = "level_name")
    private String levelName;
}
