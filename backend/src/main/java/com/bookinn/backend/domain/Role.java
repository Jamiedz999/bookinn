package com.bookinn.backend.domain;

/** Authorization roles a user can hold. Persisted by name in the {@code user_role} table. */
public enum Role {
  GUEST,
  HOST
}
