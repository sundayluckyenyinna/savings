package com.accionmfb.omnix.savings.target_saving.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "role_groups")
public class RoleGroups
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "group_name")
    private String groupName;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
