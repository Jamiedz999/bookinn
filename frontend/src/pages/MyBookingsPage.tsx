import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  Container,
  Stack,
  Typography,
} from '@mui/material'
import { cancelBooking, getMyBookings } from '../api/bookings'
import type { BookingStatus } from '../api/types'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price)
}

function statusColor(status: BookingStatus): 'success' | 'default' | 'error' {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'CANCELLED') return 'error'
  return 'default'
}

export function MyBookingsPage() {
  const queryClient = useQueryClient()
  const bookingsQuery = useQuery({ queryKey: ['my-bookings'], queryFn: getMyBookings })
  const cancelMutation = useMutation({
    mutationFn: (id: number) => cancelBooking(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['my-bookings'] }),
  })

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 6 }}>
        <Typography variant="h4" gutterBottom>
          My bookings
        </Typography>

        {bookingsQuery.isLoading && <Typography>Loading…</Typography>}
        {bookingsQuery.isError && <Alert severity="error">Could not load your bookings.</Alert>}
        {bookingsQuery.data?.length === 0 && (
          <Typography color="text.secondary">You have no bookings yet.</Typography>
        )}
        {cancelMutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Could not cancel — free cancellation ends 48h before check-in.
          </Alert>
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
                  {booking.listingCity} · {booking.checkIn} → {booking.checkOut} ·{' '}
                  {booking.nights} night{booking.nights === 1 ? '' : 's'}
                </Typography>
                <Typography sx={{ mt: 1 }}>{formatPrice(booking.totalPrice)}</Typography>
              </CardContent>
              <CardActions>
                <Button
                  size="small"
                  color="error"
                  disabled={!booking.cancellable || cancelMutation.isPending}
                  onClick={() => cancelMutation.mutate(booking.id)}
                >
                  Cancel
                </Button>
              </CardActions>
            </Card>
          ))}
        </Stack>
      </Box>
    </Container>
  )
}
