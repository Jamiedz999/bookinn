package com.bookinn.backend.service;

/**
 * The two protected demo personas the public sandbox exposes via one-click login (D7). Both the
 * demo seed (M6) and {@link AuthService#demoLogin} key off these emails, so they must agree: the
 * seed creates the accounts up front, and demo-login simply finds them. The password is fixed and
 * published in the README so an interviewer can also sign in through the normal login form; the
 * accounts are flagged {@code is_demo}, so they are protected from email/password changes and wiped
 * nightly regardless.
 */
public final class DemoAccounts {

  /** Email of the demo host persona (has both GUEST and HOST roles). */
  public static final String HOST_EMAIL = "demo-host@bookinn.app";

  /** Email of the demo guest persona (GUEST only). */
  public static final String GUEST_EMAIL = "demo-guest@bookinn.app";

  /** Published password for both demo personas, so the login form works as well as the button. */
  public static final String PASSWORD = "demo1234";

  private DemoAccounts() {}
}
