package com.diegoramos.mylifediary.modules.auth.controller;

import com.diegoramos.mylifediary.common.result.Result;
import com.diegoramos.mylifediary.modules.auth.dto.request.LoginRequest;
import com.diegoramos.mylifediary.modules.auth.dto.request.RefreshRequest;
import com.diegoramos.mylifediary.modules.auth.dto.response.AuthResponse;
import com.diegoramos.mylifediary.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerIntegrationTest {

	@Mock
	private AuthService authService;

	@InjectMocks
	private AuthController authController;

	@Test
	void loginShouldReturnTokenPairWithOkStatus() {
		AuthResponse response = new AuthResponse("access", "refresh", "Bearer", 3600L);
		when(authService.authenticate(new LoginRequest("john@example.com", "secret"))).thenReturn(Result.success(response));

		ResponseEntity<?> entity = authController.login(new LoginRequest("john@example.com", "secret"));

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertInstanceOf(AuthResponse.class, entity.getBody());
		assertEquals("refresh", ((AuthResponse) entity.getBody()).refreshToken());
	}

	@Test
	void refreshShouldReturnRotatedTokenPairWithOkStatus() {
		AuthResponse response = new AuthResponse("new-access", "new-refresh", "Bearer", 3600L);
		when(authService.refresh(new RefreshRequest("old-refresh"))).thenReturn(Result.success(response));

		ResponseEntity<?> entity = authController.refresh(new RefreshRequest("old-refresh"));

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertInstanceOf(AuthResponse.class, entity.getBody());
		assertEquals("new-refresh", ((AuthResponse) entity.getBody()).refreshToken());
	}

	@Test
	void logoutShouldReturnNoContentWhenSuccessful() {
		when(authService.logout(new RefreshRequest("tok"))).thenReturn(Result.success(null));

		ResponseEntity<?> entity = authController.logout(new RefreshRequest("tok"));

		assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
		assertEquals(null, entity.getBody());
	}

	@Test
	void refreshFailureShouldReturnUnauthorized() {
		when(authService.refresh(new RefreshRequest("invalid"))).thenReturn(Result.failure("AUTH_REFRESH_TOKEN_EXPIRED", "Refresh token expirado"));

		ResponseEntity<?> entity = authController.refresh(new RefreshRequest("invalid"));

		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
	}
}
