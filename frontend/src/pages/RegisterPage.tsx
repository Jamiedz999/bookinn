import { type FormEvent, useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Container,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useAuth } from '../auth/useAuth'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register({ name, email, password })
      navigate('/login')
    } catch {
      setError('Registration failed. The email may already be in use.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Container maxWidth="sm">
      <Box component="form" onSubmit={handleSubmit} sx={{ mt: 8 }}>
        <Typography variant="h4" gutterBottom>
          Create your BookInn account
        </Typography>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Stack spacing={2}>
          <TextField
            label="Name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
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
            Register
          </Button>
        </Stack>
      </Box>

      <Typography sx={{ mt: 3 }}>
        Already have an account?{' '}
        <Link component={RouterLink} to="/login">
          Sign in
        </Link>
      </Typography>
    </Container>
  )
}
