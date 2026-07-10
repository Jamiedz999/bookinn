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
        <Button variant="outlined" onClick={() => logout()}>
          Log out
        </Button>
      </Box>
    </Container>
  )
}
