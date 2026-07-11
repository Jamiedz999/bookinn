import { type FormEvent, useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Container,
  Divider,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import type { Role } from '../api/types'
import { useAuth } from '../auth/useAuth'

export function LoginPage() {
  const { login, demoLogin } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      navigate('/')
    } catch {
      setError('Invalid email or password')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDemo(role: Role) {
    setError(null)
    setSubmitting(true)
    try {
      await demoLogin(role)
      navigate('/')
    } catch {
      setError('Demo login failed, please try again')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Container maxWidth="sm">
      <Box component="form" onSubmit={handleSubmit} sx={{ mt: 8 }}>
        <Typography variant="h4" gutterBottom>
          Sign in to BookInn
        </Typography>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Stack spacing={2}>
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          <Button type="submit" variant="contained" disabled={submitting}>
            Sign in
          </Button>
        </Stack>
      </Box>

      <Divider sx={{ my: 3 }}>or try a demo</Divider>
      <Stack direction="row" spacing={2}>
        <Button variant="outlined" fullWidth onClick={() => handleDemo('GUEST')} disabled={submitting}>
          Try as Guest
        </Button>
        <Button variant="outlined" fullWidth onClick={() => handleDemo('HOST')} disabled={submitting}>
          Try as Host
        </Button>
      </Stack>

      <Typography sx={{ mt: 3 }}>
        No account?{' '}
        <Link component={RouterLink} to="/register">
          Register
        </Link>
      </Typography>
    </Container>
  )
}
