package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.ChangeEmailRequest;
import com.bookinn.backend.dto.ChangePasswordRequest;
import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.exception.DemoAccountProtectedException;
import com.bookinn.backend.exception.EmailAlreadyExistsException;
import com.bookinn.backend.exception.InvalidCredentialsException;
import com.bookinn.backend.repository.UserRepository;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService service() {
    return new UserService(userRepository, passwordEncoder);
  }

  private static User user(String email, boolean demo) {
    User user = new User(email, "hashed", "Alice", demo, EnumSet.of(Role.GUEST));
    user.setId(3L);
    return user;
  }

  @Test
  void getByIdReturnsProfile() {
    when(userRepository.findById(3L)).thenReturn(Optional.of(user("alice@example.com", false)));

    UserResponse response = service().getById(3L);

    assertThat(response.id()).isEqualTo(3L);
    assertThat(response.name()).isEqualTo("Alice");
    assertThat(response.roles()).containsExactly(Role.GUEST);
  }

  @Test
  void getByIdThrowsWhenMissing() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getById(99L))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void changePasswordReplacesHashWhenCurrentMatches() {
    User user = user("alice@example.com", false);
    when(userRepository.findById(3L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("old-pass", "hashed")).thenReturn(true);
    when(passwordEncoder.encode("new-pass-123")).thenReturn("new-hash");

    service().changePassword(3L, new ChangePasswordRequest("old-pass", "new-pass-123"));

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    verify(userRepository).save(user);
  }

  @Test
  void changePasswordRejectsWrongCurrentPassword() {
    User user = user("alice@example.com", false);
    when(userRepository.findById(3L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

    assertThatThrownBy(
            () -> service().changePassword(3L, new ChangePasswordRequest("wrong", "new-pass-123")))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void changePasswordRefusesDemoAccountWith403() {
    when(userRepository.findById(3L)).thenReturn(Optional.of(user("demo@bookinn.app", true)));

    assertThatThrownBy(
            () -> service().changePassword(3L, new ChangePasswordRequest("old-pass", "new-pass-123")))
        .isInstanceOf(DemoAccountProtectedException.class);
    verify(passwordEncoder, never()).matches(any(), any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void changeEmailUpdatesWhenNewEmailIsFree() {
    User user = user("alice@example.com", false);
    when(userRepository.findById(3L)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(user)).thenReturn(user);

    UserResponse response = service().changeEmail(3L, new ChangeEmailRequest("new@example.com"));

    assertThat(response.email()).isEqualTo("new@example.com");
    assertThat(user.getEmail()).isEqualTo("new@example.com");
  }

  @Test
  void changeEmailRejectsAlreadyTakenEmail() {
    User user = user("alice@example.com", false);
    when(userRepository.findById(3L)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(() -> service().changeEmail(3L, new ChangeEmailRequest("taken@example.com")))
        .isInstanceOf(EmailAlreadyExistsException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void changeEmailRefusesDemoAccountWith403() {
    when(userRepository.findById(3L)).thenReturn(Optional.of(user("demo@bookinn.app", true)));

    assertThatThrownBy(() -> service().changeEmail(3L, new ChangeEmailRequest("new@example.com")))
        .isInstanceOf(DemoAccountProtectedException.class);
    verify(userRepository, never()).save(any());
  }
}
