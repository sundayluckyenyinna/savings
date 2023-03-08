package com.accionmfb.omnix.savings.target_saving.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class AppUser
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "locked")
    private boolean isLocked = false;
    @Column(name = "expired")
    private boolean isExpired = false;
    @Column(name = "enabled")
    private boolean isEnabled = true;
    @OneToOne
    private ConnectingIP connectingIP;
    @ManyToOne
    private RoleGroups role;
    @Column(name = "channel")
    private String channel;
    @Column(name = "password_change_date")
    private LocalDate passwordChangeDate;
    @Column(name = "encryption_key")
    private String encryptionKey;
    @Column(name = "pay_account_open_bonus")
    private boolean payAccountOpenBonus = false;
    @Column(name = "account_open_bonus")
    private String accountOpenBonus = "";
    @Column(name = "account_number")
    private String accountNumber = "";
}
