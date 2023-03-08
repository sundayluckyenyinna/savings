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
@Table(name = "group_roles")
public class GroupRoles
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @ManyToOne
    private AppRoles appRole;
    @ManyToOne
    private RoleGroups roleGroup;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
