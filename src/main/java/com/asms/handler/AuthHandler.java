package com.asms.handler;

import com.asms.api.AuthApiDelegate;
import com.asms.model.ChangePasswordRequestDto;
import com.asms.model.LoginRequestDto;
import com.asms.model.LoginResponseDto;
import com.asms.model.MfaEnrollmentResponseDto;
import com.asms.model.MfaVerifyRequestDto;
import com.asms.model.MfaVerifyResponseDto;
import com.asms.model.SelectOrgRequestDto;
import com.asms.model.SelectOrgResponseDto;
import com.asms.service.AuthService;
import com.asms.service.AuthService.LoginResult;
import com.asms.service.AuthService.LoginStatus;
import com.asms.service.AuthService.OrgSelectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * REST adapter for the Auth API.
 *
 * <p>Implements {@link AuthApiDelegate} — the contract-generated delegation interface.
 * Translates HTTP concerns (DTO in/out, response status) and delegates all business
 * logic to {@link AuthService}.
 *
 * <p>No business logic lives here. Services return domain objects; this class
 * maps them to DTOs using inline mapping (Auth flow DTOs are thin enough not to
 * require a dedicated MapStruct mapper).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler implements AuthApiDelegate {

    private final AuthService authService;

    @Override
    public ResponseEntity<LoginResponseDto> login(LoginRequestDto loginRequestDto) {
        LoginResult result = authService.login(loginRequestDto);
        LoginResponseDto response = new LoginResponseDto();
        response.setStatus(mapLoginStatus(result.status()));
        // Session token carries userId for the downstream org-selection step
        response.setSessionToken(result.user().getId().toString());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<LoginResponseDto> changePassword(ChangePasswordRequestDto changePasswordRequestDto) {
        var user = authService.changePassword(changePasswordRequestDto);
        LoginResponseDto response = new LoginResponseDto();
        response.setStatus(user.isMfaEnabled()
                ? LoginResponseDto.StatusEnum.MFA_REQUIRED
                : LoginResponseDto.StatusEnum.ORG_SELECTION_REQUIRED);
        // Pass the sessionToken forward so the next step (org-selection / MFA) can identify the user.
        response.setSessionToken(user.getId().toString());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MfaEnrollmentResponseDto> enrollMfa() {
        return ResponseEntity.ok(authService.enrollMfa());
    }

    @Override
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SelectOrgResponseDto> selectOrganization(SelectOrgRequestDto selectOrgRequestDto) {
        OrgSelectionResult result = authService.selectOrganization(selectOrgRequestDto);
        SelectOrgResponseDto response = new SelectOrgResponseDto();
        response.setAccessToken(result.accessToken());
        response.setExpiresIn(result.expiresInSeconds());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MfaVerifyResponseDto> verifyMfa(MfaVerifyRequestDto mfaVerifyRequestDto) {
        return ResponseEntity.ok(authService.verifyMfa(mfaVerifyRequestDto));
    }

    // ─── private mapping helpers ─────────────────────────────────────────────

    private LoginResponseDto.StatusEnum mapLoginStatus(LoginStatus status) {
        return switch (status) {
            case TEMP_PASSWORD_REQUIRED -> LoginResponseDto.StatusEnum.TEMP_PASSWORD_REQUIRED;
            case MFA_REQUIRED -> LoginResponseDto.StatusEnum.MFA_REQUIRED;
            case ORG_SELECTION_REQUIRED -> LoginResponseDto.StatusEnum.ORG_SELECTION_REQUIRED;
        };
    }
}
