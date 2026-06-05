package com.asms.service;

import com.asms.domain.Permission;
import com.asms.domain.enums.PermissionStatus;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.PermissionImportRepository;
import com.asms.repository.PermissionRepository;
import com.asms.service.EffectivePermissionService;
import com.asms.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsServiceTest {

    @Mock PermissionRepository permissionRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock PermissionImportRepository permissionImportRepository;
    @Mock EffectivePermissionService effectivePermissionService;
    @Mock AuditService auditService;
    @InjectMocks PermissionsService service;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID orgId;

    @BeforeEach
    void setUp() {
        // inject ObjectMapper manually since @InjectMocks doesn't inject non-mock fields
        try {
            var field = PermissionsService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(service, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        orgId = UUID.randomUUID();
    }

    @Test
    void exportPermissions_returnsHeaderRow_whenNoResults() {
        when(permissionRepository.findByOrganizationId(eq(orgId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        byte[] csv = service.exportPermissions(orgId, null, null);

        String result = new String(csv);
        assertThat(result).startsWith("id,name,resource,action,status,description,organizationId,createdAt,updatedAt");
    }

    @Test
    void exportPermissions_returnsDataRow_whenPermissionsExist() {
        var org = new com.asms.domain.Organization();
        org.setId(orgId);
        Permission p = Permission.builder()
                .id(UUID.randomUUID())
                .name("hr.employee.read")
                .resource("hr.employee")
                .action("READ")
                .status(PermissionStatus.ACTIVE)
                .description("Read access")
                .organization(org)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(permissionRepository.findByOrganizationId(eq(orgId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        byte[] csv = service.exportPermissions(orgId, null, null);

        String result = new String(csv);
        assertThat(result).contains("hr.employee.read");
        assertThat(result).contains("hr.employee");
        assertThat(result).contains("READ");
    }

    @Test
    void exportPermissions_filtersWithStatus_whenStatusProvided() {
        when(permissionRepository.findByOrganizationIdAndStatus(
                eq(orgId), eq(PermissionStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        byte[] csv = service.exportPermissions(orgId, null, "ACTIVE");

        assertThat(csv).isNotEmpty();
    }
}
