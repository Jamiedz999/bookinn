import { Link as RouterLink } from 'react-router-dom'
import { Box, Button, Chip, Container, Stack, Typography } from '@mui/material'
import { useAuth } from '../auth/useAuth'

export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8 }}>
        <Typography variant="h4" gutterBottom>
          Welcome, {user?.name}
        </Typography>
        <Stack direction="row" spacing={1} sx={{ mb: 3 }}>
          {user?.roles.map((role) => (
            <Chip key={role} label={role} color="primary" variant="outlined" />
          ))}
        </Stack>
        <Stack direction="row" spacing={2}>
          {/* Guest entries belong to every logged-in user — Guest is the baseline role. */}
          <Button component={RouterLink} to="/search" variant="contained">
            Browse stays
          </Button>
          <Button component={RouterLink} to="/bookings/my" variant="outlined">
            My bookings
          </Button>
          {/* Host is an additive capability: only show host entries to Hosts. */}
          {user?.roles.includes('HOST') && (
            <>
              <Button component={RouterLink} to="/host/listings" variant="outlined">
                My listings
              </Button>
              <Button component={RouterLink} to="/host/dashboard" variant="outlined">
                Dashboard
              </Button>
            </>
          )}
          <Button variant="outlined" onClick={() => logout()}>
            Log out
          </Button>
        </Stack>
      </Box>
    </Container>
  )
}
