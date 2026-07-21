import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  Container,
  Stack,
  Typography,
} from '@mui/material'
import { getHostBookings } from '../api/bookings'
import type { BookingStatus } from '../api/types'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price)
}

function statusColor(status: BookingStatus): 'success' | 'default' | 'error' {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'CANCELLED') return 'error'
  return 'default'
}

export function HostBookingsPage() {
  const bookingsQuery = useQuery({ queryKey: ['host-bookings'], queryFn: getHostBookings })

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 6 }}>
        <Typography variant="h4" gutterBottom>
          Bookings received
        </Typography>

        {bookingsQuery.isLoading && <Typography>Loading…</Typography>}
        {bookingsQuery.isError && <Alert severity="error">Could not load bookings.</Alert>}
        {bookingsQuery.data?.length === 0 && (
          <Typography color="text.secondary">No bookings on your listings yet.</Typography>
        )}

        <Stack spacing={2}>
          {bookingsQuery.data?.map((booking) => (
            <Card key={booking.id} variant="outlined">
              <CardContent>
                <Stack
                  direction="row"
                  sx={{ justifyContent: 'space-between', alignItems: 'center' }}
                >
                  <Typography variant="h6">{booking.listingTitle}</Typography>
                  <Chip
                    label={booking.status}
                    color={statusColor(booking.status)}
                    size="small"
                  />
                </Stack>
                <Typography color="text.secondary">
                  {booking.guestName} · {booking.checkIn} → {booking.checkOut} ·{' '}
                  {booking.guestCount} guest{booking.guestCount === 1 ? '' : 's'}
                </Typography>
                <Typography sx={{ mt: 1 }}>{formatPrice(booking.totalPrice)}</Typography>
              </CardContent>
            </Card>
          ))}
        </Stack>
      </Box>
    </Container>
  )
}
