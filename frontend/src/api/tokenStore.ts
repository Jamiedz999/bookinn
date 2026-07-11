// The access token lives only in memory (never localStorage) so it is not readable by injected
// scripts. It is lost on reload and re-minted from the httpOnly refresh cookie on app bootstrap.
let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}
