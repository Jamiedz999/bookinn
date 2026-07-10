package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.exception.InvalidCredentialsException;
import com.bookinn.backend.repository.UserRepository;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Test
  void getByIdReturnsProfile() {
    User user = new User("alice@example.com", "enc", "Alice", false, EnumSet.of(Role.GUEST));
    user.setId(3L);
    when(userRepository.findById(3L)).thenReturn(Optional.of(user));

    UserResponse response = new UserService(userRepository).getById(3L);

    assertThat(response.id()).isEqualTo(3L);
    assertThat(response.name()).isEqualTo("Alice");
    assertThat(response.roles()).containsExactly(Role.GUEST);
  }

  @Test
  void getByIdThrowsWhenMissing() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> new UserService(userRepository).getById(99L))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
