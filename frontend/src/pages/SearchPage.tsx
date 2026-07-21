import { type FormEvent, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Container,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { searchListings } from '../api/listings'
import type { SearchParams } from '../api/types'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price)
}

export function SearchPage() {
  const [city, setCity] = useState('')
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [params, setParams] = useState<SearchParams>({})
  const [formError, setFormError] = useState<string | null>(null)

  const searchQuery = useQuery({
    queryKey: ['search', params],
    queryFn: () => searchListings(params),
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    // A one-sided date window is a 400 on the server; guard it in the UI instead.
    if (Boolean(checkIn) !== Boolean(checkOut)) {
      setFormError('Please choose both check-in and check-out dates, or neither.')
      return
    }
    setFormError(null)
    const next: SearchParams = { page: 0 }
    if (city.trim()) {
      next.city = city.trim()
    }
    if (checkIn && checkOut) {
      next.checkIn = checkIn
      next.checkOut = checkOut
    }
    setParams(next)
  }

  const listings = searchQuery.data?.content ?? []

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 6 }}>
        <Typography variant="h4" gutterBottom>
          Find a place to stay
        </Typography>

        <Box component="form" onSubmit={handleSubmit} sx={{ mb: 3 }}>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            sx={{ alignItems: { sm: 'center' } }}
          >
            <TextField
              label="City"
              value={city}
              onChange={(e) => setCity(e.target.value)}
              fullWidth
            />
            <TextField
              label="Check-in"
              type="date"
              value={checkIn}
              onChange={(e) => setCheckIn(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              label="Check-out"
              type="date"
              value={checkOut}
              onChange={(e) => setCheckOut(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <Button type="submit" variant="contained">
              Search
            </Button>
          </Stack>
        </Box>

        {formError && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            {formError}
          </Alert>
        )}
        {searchQuery.isLoading && <Typography>Loading…</Typography>}
        {searchQuery.isError && <Alert severity="error">Could not load listings.</Alert>}
        {searchQuery.isSuccess && listings.length === 0 && (
          <Typography color="text.secondary">No places match your search.</Typography>
        )}

        <Stack spacing={2}>
          {listings.map((listing) => (
            <Card key={listing.id} variant="outlined">
              <CardActionArea component={RouterLink} to={`/listings/${listing.id}`}>
                <CardContent>
                  <Typography variant="h6">{listing.title}</Typography>
                  <Typography color="text.secondary">
                    {listing.city} · {formatPrice(listing.pricePerNight)} / night
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
        </Stack>
      </Box>
    </Container>
  )
}
