package com.asms.domain;

import com.asms.domain.enums.OrganizationPlan;
import com.asms.domain.enums.OrganizationStatus;
import com.asms.domain.enums.converter.OrganizationPlanConverter;
import com.asms.domain.enums.converter.OrganizationStatusConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "domain", unique = true)
    private String domain;

    @Column(name = "description")
    private String description;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "display_name")
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_org_id")
    private Organization parentOrganization;

    @Column(name = "logo_url")
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;


    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "status", nullable = false)
    @Convert(converter = OrganizationStatusConverter.class)
    private OrganizationStatus status;

    @Column(name = "plan", nullable = false)
    @Convert(converter = OrganizationPlanConverter.class)
    private OrganizationPlan plan;

    @Column(name = "country", nullable = false, updatable = false)
    private String country;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<Membership> memberships;
}
