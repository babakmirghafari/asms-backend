package com.asms.constant;

/**
 * Named constants for all audit action codes.
 *
 * <p>Replaces inline string literals throughout service classes.
 * Convention: {ENTITY}_{VERB} for state changes, ACTIVITY_{VERB} for user activity.
 */
public final class AuditActions {

    private AuditActions() {}

    // User lifecycle
    public static final String USER_CREATED           = "USER_CREATED";
    public static final String USER_UPDATED           = "USER_UPDATED";
    public static final String USER_DELETED           = "USER_DELETED";
    public static final String USER_STATUS_CHANGED    = "USER_STATUS_CHANGED";
    public static final String PASSWORD_CHANGED       = "PASSWORD_CHANGED";

    // Auth events
    public static final String ACTIVITY_LOGIN_SUCCESS              = "ACTIVITY_LOGIN_SUCCESS";
    public static final String ACTIVITY_LOGOUT                     = "ACTIVITY_LOGOUT";
    public static final String LOGIN_FAILED_USER_NOT_FOUND         = "LOGIN_FAILED_USER_NOT_FOUND";
    public static final String LOGIN_FAILED_WRONG_PASSWORD         = "LOGIN_FAILED_WRONG_PASSWORD";
    public static final String LOGIN_FAILED_ACCOUNT_LOCKED         = "LOGIN_FAILED_ACCOUNT_LOCKED";
    public static final String LOGIN_FAILED_INVALID_STATUS         = "LOGIN_FAILED_INVALID_STATUS";

    // Alert lifecycle
    public static final String ALERT_ACKNOWLEDGED    = "ALERT_ACKNOWLEDGED";
    public static final String ALERT_RESOLVED        = "ALERT_RESOLVED";
    public static final String ALERT_ESCALATED       = "ALERT_ESCALATED";

    // Session lifecycle
    public static final String SESSION_REVOKED       = "SESSION_REVOKED";
    public static final String ALL_SESSIONS_REVOKED  = "ALL_SESSIONS_REVOKED";

    // Permission lifecycle
    public static final String PERMISSION_CREATED           = "PERMISSION_CREATED";
    public static final String PERMISSION_DELETED           = "PERMISSION_DELETED";
    public static final String PERMISSION_STATUS_CHANGED    = "PERMISSION_STATUS_CHANGED";
    public static final String PERMISSION_CREATED_VIA_IMPORT = "PERMISSION_CREATED_VIA_IMPORT";
}
