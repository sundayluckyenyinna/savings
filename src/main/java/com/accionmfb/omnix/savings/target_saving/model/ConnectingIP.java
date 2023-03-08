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
@Table(name = "connecting_ip")
public class ConnectingIP
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "organization")
    private String organization;
    @Column(name = "organization_address")
    private String organizationAddress;
    @Column(name = "organization_contact")
    private String organizationContact;
    @Column(name = "organization_contact_person")
    private String organizationContactPerson;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
