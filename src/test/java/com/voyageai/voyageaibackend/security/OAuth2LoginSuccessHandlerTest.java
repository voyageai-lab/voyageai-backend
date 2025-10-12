package com.voyageai.voyageaibackend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link OAuth2LoginSuccessHandler}.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @InjectMocks
  private OAuth2LoginSuccessHandler handler;

  private OAuth2AuthenticationToken authenticationToken;
  private OAuth2User oauth2User;

  @BeforeEach
  void setUp() {
    // Set frontend URL
    ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");

    // Create OAuth2 user attributes
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("sub", "google-user-123");
    attributes.put("email", "test@example.com");
    attributes.put("name", "Test User");
    attributes.put("picture", "https://example.com/avatar.jpg");

    // Create OAuth2 user
    oauth2User = new DefaultOAuth2User(
        java.util.Collections.singleton(new OAuth2UserAuthority(attributes)),
        attributes,
        "sub"
    );

    // Create authentication token
    authenticationToken = new OAuth2AuthenticationToken(
        oauth2User,
        oauth2User.getAuthorities(),
        "google"
    );
  }

  @Test
  void onAuthenticationSuccess_newUser_createsUserAndRedirects() throws Exception {
    // Arrange
    when(userRepository.findByAuthProviderAndProviderUserId(any(), anyString()))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("test.jwt.token");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setEmail("test@example.com");
    savedUser.setDisplayName("Test User");
    savedUser.setAuthProvider(User.AuthProvider.GOOGLE);
    savedUser.setProviderUserId("google-user-123");
    savedUser.setCreatedAt(Instant.now());

    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    handler.onAuthenticationSuccess(request, response, authenticationToken);

    // Assert
    verify(userRepository).findByAuthProviderAndProviderUserId(User.AuthProvider.GOOGLE, "google-user-123");
    verify(userRepository).save(any(User.class));
    verify(jwtUtil).generateToken("test@example.com", 1L);
    verify(response).sendRedirect("http://localhost:3000/auth/callback?token=test.jwt.token");
  }

  @Test
  void onAuthenticationSuccess_existingGoogleUser_usesExistingAccount() throws Exception {
    // Arrange
    User existingUser = new User();
    existingUser.setId(1L);
    existingUser.setEmail("test@example.com");
    existingUser.setDisplayName("Old Name");
    existingUser.setAuthProvider(User.AuthProvider.GOOGLE);
    existingUser.setProviderUserId("google-user-123");
    existingUser.setCreatedAt(Instant.now());

    when(userRepository.findByAuthProviderAndProviderUserId(User.AuthProvider.GOOGLE, "google-user-123"))
        .thenReturn(Optional.of(existingUser));
    when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("test.jwt.token");

    // Act
    handler.onAuthenticationSuccess(request, response, authenticationToken);

    // Assert
    verify(userRepository).findByAuthProviderAndProviderUserId(User.AuthProvider.GOOGLE, "google-user-123");
    // When user already exists with correct provider, no save is needed
    verify(userRepository, never()).save(any(User.class));
    verify(jwtUtil).generateToken("test@example.com", 1L);
    verify(response).sendRedirect("http://localhost:3000/auth/callback?token=test.jwt.token");
  }

  @Test
  void onAuthenticationSuccess_existingLocalUser_mergesAccount() throws Exception {
    // Arrange
    User localUser = new User();
    localUser.setId(1L);
    localUser.setEmail("test@example.com");
    localUser.setPasswordHash("hashedPassword");
    localUser.setDisplayName("Local User");
    localUser.setAuthProvider(User.AuthProvider.LOCAL);
    localUser.setProviderUserId(null);
    localUser.setCreatedAt(Instant.now());

    when(userRepository.findByAuthProviderAndProviderUserId(User.AuthProvider.GOOGLE, "google-user-123"))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(localUser));
    when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("test.jwt.token");

    User mergedUser = localUser;
    mergedUser.setProviderUserId("google-user-123");
    when(userRepository.save(any(User.class))).thenReturn(mergedUser);

    // Act
    handler.onAuthenticationSuccess(request, response, authenticationToken);

    // Assert
    verify(userRepository).findByAuthProviderAndProviderUserId(User.AuthProvider.GOOGLE, "google-user-123");
    verify(userRepository).findByEmail("test@example.com");
    verify(userRepository).save(any(User.class));
    verify(jwtUtil).generateToken("test@example.com", 1L);
    verify(response).sendRedirect("http://localhost:3000/auth/callback?token=test.jwt.token");
  }

  @Test
  void onAuthenticationSuccess_noEmail_throwsNullPointerException() {
    // Arrange - OAuth2 user without email
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("sub", "google-user-123");
    attributes.put("name", "Test User");
    // No email attribute

    OAuth2User userWithoutEmail = new DefaultOAuth2User(
        java.util.Collections.singleton(new OAuth2UserAuthority(attributes)),
        attributes,
        "sub"
    );

    OAuth2AuthenticationToken tokenWithoutEmail = new OAuth2AuthenticationToken(
        userWithoutEmail,
        userWithoutEmail.getAuthorities(),
        "google"
    );

    // Act & Assert
    // Note: Current implementation doesn't handle null email gracefully
    // This would throw NullPointerException in production
    // TODO: Add null check in OAuth2LoginSuccessHandler
    org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
      handler.onAuthenticationSuccess(request, response, tokenWithoutEmail);
    });
  }

  @Test
  void onAuthenticationSuccess_googleProvider_setsCorrectProviderType() throws Exception {
    // Arrange
    when(userRepository.findByAuthProviderAndProviderUserId(any(), anyString()))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("test.jwt.token");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setEmail("test@example.com");
    savedUser.setAuthProvider(User.AuthProvider.GOOGLE);
    savedUser.setProviderUserId("google-user-123");

    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      // Verify that GOOGLE provider is set
      org.junit.jupiter.api.Assertions.assertEquals(User.AuthProvider.GOOGLE, user.getAuthProvider());
      return savedUser;
    });

    // Act
    handler.onAuthenticationSuccess(request, response, authenticationToken);

    // Assert
    verify(userRepository, times(1)).save(any(User.class));
  }
}

