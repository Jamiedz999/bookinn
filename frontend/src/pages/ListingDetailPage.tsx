import { useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Chip,
  Container,
  Divider,
  ImageList,
  ImageListItem,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { createBooking, getQuote } from '../api/bookings'
import { getListing } from '../api/listings'
import { useAuth } from '../auth/useAuth'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price)
}

export function ListingDetailPage() {
  const { id } = useParams<{ id: string }>()
  const listingId = Number(id)
  const {
    data: listing,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['listing', listingId],
    queryFn: () => getListing(listingId),
    enabled: Number.isFinite(listingId),
  })

  if (isLoading) {
    return (
      <Container maxWidth="md">
        <Typography sx={{ mt: 8 }}>Loading…</Typography>
      </Container>
    )
  }

  if (isError || !listing) {
    return (
      <Container maxWidth="md">
        <Alert severity="error" sx={{ mt: 8 }}>
          Listing not found
        </Alert>
      </Container>
    )
  }

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 6 }}>
        <Typography variant="h4" gutterBottom>
          {listing.title}
        </Typography>
        <Typography variant="subtitle1" color="text.secondary" gutterBottom>
          {listing.city} · {listing.address}
        </Typography>
        <Typography variant="h6" sx={{ mt: 1 }}>
          {formatPrice(listing.pricePerNight)} / night · up to {listing.maxGuests} guests
        </Typography>

        {listing.photoUrls.length > 0 && (
          <ImageList cols={3} gap={8} sx={{ mt: 2 }}>
            {listing.photoUrls.map((url) => (
              <ImageListItem key={url}>
                <img src={url} alt={listing.title} loading="lazy" />
              </ImageListItem>
            ))}
          </ImageList>
        )}

        {listing.description && <Typography sx={{ mt: 2 }}>{listing.description}</Typography>}

        {listing.amenities.length > 0 && (
          <Box sx={{ mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              Amenities
            </Typography>
            <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
              {listing.amenities.map((amenity) => (
                <Chip key={amenity.id} label={amenity.name} />
              ))}
            </Stack>
          </Box>
        )}

        <Divider sx={{ my: 4 }} />
        <BookingPanel listingId={listing.id} maxGuests={listing.maxGuests} />
      </Box>
    </Container>
  )
}

/** Date picker → quote → book flow. Gated on being logged in. */
function BookingPanel({ listingId, maxGuests }: { listingId: number; maxGuests: number }) {
  const { user } = useAuth()
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [guestCount, setGuestCount] = useState(1)

  const quoteMutation = useMutation({
    mutationFn: () => getQuote(listingId, checkIn, checkOut),
  })
  const bookMutation = useMutation({
    mutationFn: () => createBooking({ listingId, checkIn, checkOut, guestCount }),
  })

  // Any change to the dates invalidates a previous quote/booking outcome.
  function resetOutcomes() {
    quoteMutation.reset()
    bookMutation.reset()
  }

  const datesChosen = Boolean(checkIn) && Boolean(checkOut)

  if (!user) {
    return (
      <Alert severity="info">
        <Link component={RouterLink} to="/login">
          Log in
        </Link>{' '}
        to book this place.
      </Alert>
    )
  }

  if (bookMutation.isSuccess) {
    return (
      <Alert severity="success">
        Booking confirmed!{' '}
        <Link component={RouterLink} to="/bookings/my">
          View my bookings
        </Link>
      </Alert>
    )
  }

  const quote = quoteMutation.data

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Book your stay
      </Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: 'center' }}>
        <TextField
          label="Check-in"
          type="date"
          value={checkIn}
          onChange={(e) => {
            setCheckIn(e.target.value)
            resetOutcomes()
          }}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <TextField
          label="Check-out"
          type="date"
          value={checkOut}
          onChange={(e) => {
            setCheckOut(e.target.value)
            resetOutcomes()
          }}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <TextField
          label="Guests"
          type="number"
          value={guestCount}
          onChange={(e) => setGuestCount(Number(e.target.value))}
          slotProps={{ htmlInput: { min: 1, max: maxGuests } }}
        />
        <Button
          variant="outlined"
          disabled={!datesChosen || quoteMutation.isPending}
          onClick={() => quoteMutation.mutate()}
        >
          Get price
        </Button>
      </Stack>

      {quoteMutation.isError && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          These dates aren't available. Try different ones.
        </Alert>
      )}

      {quote && (
        <Box sx={{ mt: 2 }}>
          <Typography>
            {quote.nights} night{quote.nights === 1 ? '' : 's'} ×{' '}
            {formatPrice(quote.pricePerNight)} ={' '}
            <strong>{formatPrice(quote.totalPrice)}</strong>
          </Typography>
          {bookMutation.isError && (
            <Alert severity="error" sx={{ mt: 1 }}>
              Could not complete the booking — the dates may have just been taken.
            </Alert>
          )}
          <Button
            variant="contained"
            sx={{ mt: 2 }}
            disabled={bookMutation.isPending}
            onClick={() => bookMutation.mutate()}
          >
            Book for {formatPrice(quote.totalPrice)}
          </Button>
        </Box>
      )}
    </Box>
  )
}
