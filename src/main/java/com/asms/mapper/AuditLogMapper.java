package com.asms.mapper;

import com.asms.domain.AuditLog;
import com.asms.model.ActivityLogDto;
import com.asms.model.AuditLogEntryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link AuditLog} domain entity ↔ contract DTO types.
 *
 * <p>AuditLogEntryDto (v2.0.0): id, eventType, actorId, actorUsername, targetType,
 * targetId, organizationId, ipAddress, outcome, details, timestamp, previousHash.
 *
 * <p>ActivityLogDto (v2.0.0): id, eventType, category, actorId, actorUsername,
 * targetType, targetId, targetDisplayName, organizationId, ipAddress,
 * outcome, summary, timestamp.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "eventType", source = "action")
    @Mapping(target = "timestamp", source = "createdAt")
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "outcome", ignore = true)
    @Mapping(target = "details", ignore = true)
    AuditLogEntryDto toAuditLogEntryDto(AuditLog auditLog);

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "eventType", source = "action")
    @Mapping(target = "timestamp", source = "createdAt")
    @Mapping(target = "category", source = "action", qualifiedByName = "actionToCategory")
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "outcome", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "targetDisplayName", ignore = true)
    ActivityLogDto toActivityLogDto(AuditLog auditLog);

    /**
     * Derives ActivityLogDto.CategoryEnum from the audit log action string.
     * Returns null for unrecognised action prefixes rather than throwing.
     */
    @Named("actionToCategory")
    static ActivityLogDto.CategoryEnum actionToCategory(String action) {
        if (action == null) return null;
        if (action.startsWith("ACTIVITY_LOGIN") || action.startsWith("ACTIVITY_LOGOUT")
                || action.startsWith("ACTIVITY_MFA") || action.startsWith("ACTIVITY_PASSWORD")
                || action.startsWith("LOGIN") || action.startsWith("LOGOUT")) {
            return ActivityLogDto.CategoryEnum.AUTHENTICATION;
        }
        if (action.contains("SESSION")) {
            return ActivityLogDto.CategoryEnum.SESSION;
        }
        if (action.contains("PERMISSION") || action.contains("ACCESS")) {
            return ActivityLogDto.CategoryEnum.PERMISSION_CHANGE;
        }
        if (action.contains("USER")) {
            return ActivityLogDto.CategoryEnum.USER_MANAGEMENT;
        }
        if (action.contains("APPLICATION")) {
            return ActivityLogDto.CategoryEnum.APPLICATION;
        }
        return null;
    }
}
