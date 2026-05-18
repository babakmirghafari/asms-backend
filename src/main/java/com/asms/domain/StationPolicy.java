package com.asms.domain;

import com.asms.domain.enums.StationPolicyStatus;
import com.asms.domain.enums.converter.StationPolicyStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "station_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false)
    @Convert(converter = StationPolicyStatusConverter.class)
    private StationPolicyStatus status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_ips", columnDefinition = "text[]")
    private List<String> allowedIps;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_days", columnDefinition = "smallint[]")
    private List<Short> allowedDays;

    @Column(name = "work_hour_start")
    private Short workHourStart;

    @Column(name = "work_hour_end")
    private Short workHourEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
