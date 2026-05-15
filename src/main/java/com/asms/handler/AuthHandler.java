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
 * <p>No business logic lives here. This class must remain a thin adapter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler implements AuthApiDelegate {

    private final AuthService authService;

    @Override
    public ResponseEntity<LoginResponseDto> changePassword(ChangePasswordRequestDto changePasswordRequestDto) {
        return authService.changePassword(changePasswordRequestDto);
    }

    @Override
    public ResponseEntity<MfaEnrollmentResponseDto> enrollMfa() {
        return authService.enrollMfa();
    }

    @Override
    public ResponseEntity<LoginResponseDto> login(LoginRequestDto loginRequestDto) {
        return authService.login(loginRequestDto);
    }

    @Override
    public ResponseEntity<Void> logout() {
        return authService.logout();
    }

    @Override
    public ResponseEntity<SelectOrgResponseDto> selectOrganization(SelectOrgRequestDto selectOrgRequestDto) {
        return authService.selectOrganization(selectOrgRequestDto);
    }

    @Override
    public ResponseEntity<MfaVerifyResponseDto> verifyMfa(MfaVerifyRequestDto mfaVerifyRequestDto) {
        return authService.verifyMfa(mfaVerifyRequestDto);
    }
}
