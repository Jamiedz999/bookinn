package com.bookinn.backend.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** A registered account. Maps to the {@code users} table defined in Flyway V1. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 60)
  private String passwordHash;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "is_demo", nullable = false)
  private boolean demo;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private Set<Role> roles = EnumSet.noneOf(Role.class);

  /**
   * Creates a user with the given identity and roles.
   *
   * @param email unique login email
   * @param passwordHash BCrypt hash of the password
   * @param name display name
   * @param demo whether this is a protected demo account
   * @param roles granted roles
   */
  public User(String email, String passwordHash, String name, boolean demo, Set<Role> roles) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.name = name;
    this.demo = demo;
    this.roles = EnumSet.copyOf(roles);
  }

  /**
   * Grants a role to this user if not already present.
   *
   * @param role the role to add
   */
  public void addRole(Role role) {
    this.roles.add(role);
  }
}
