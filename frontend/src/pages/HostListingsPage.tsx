import { Link as RouterLink } from 'react-router-dom'
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
import { changeListingStatus, getHostListings } from '../api/listings'
import type { ListingStatus } from '../api/types'
import { useAuth } from '../auth/useAuth'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price)
}

export function HostListingsPage() {
  const { user, becomeHost } = useAuth()
  const isHost = user?.roles.includes('HOST') ?? false
  const queryClient = useQueryClient()

  const becomeHostMutation = useMutation({ mutationFn: () => becomeHost() })

  const listingsQuery = useQuery({
    queryKey: ['host-listings'],
    queryFn: getHostListings,
    enabled: isHost,
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: ListingStatus }) =>
      changeListingStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['host-listings'] }),
  })

  if (!isHost) {
    return (
      <Container maxWidth="sm">
        <Box sx={{ mt: 8 }}>
          <Typography variant="h4" gutterBottom>
            Become a host
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            List your place on BookInn and manage your listings.
          </Typography>
          {becomeHostMutation.isError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Could not upgrade your account. Please try again.
            </Alert>
          )}
          <Button
            variant="contained"
            onClick={() => becomeHostMutation.mutate()}
            disabled={becomeHostMutation.isPending}
          >
            Become a host
          </Button>
        </Box>
      </Container>
    )
  }

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 6 }}>
        <Stack direction="row" sx={{ mb: 3, justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h4">My listings</Typography>
          <Button component={RouterLink} to="/host/listings/new" variant="contained">
            New listing
          </Button>
        </Stack>

        {listingsQuery.isLoading && <Typography>Loading…</Typography>}
        {listingsQuery.isError && <Alert severity="error">Could not load your listings.</Alert>}
        {listingsQuery.data?.length === 0 && (
          <Typography color="text.secondary">You have no listings yet.</Typography>
        )}

        <Stack spacing={2}>
          {listingsQuery.data?.map((listing) => (
            <Card key={listing.id} variant="outlined">
              <CardContent>
                <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="h6">{listing.title}</Typography>
                  <Chip
                    label={listing.status}
                    color={listing.status === 'ACTIVE' ? 'success' : 'default'}
                    size="small"
                  />
                </Stack>
                <Typography color="text.secondary">
                  {listing.city} · {formatPrice(listing.pricePerNight)} / night
                </Typography>
              </CardContent>
              <CardActions>
                <Button component={RouterLink} to={`/host/listings/${listing.id}/edit`} size="small">
                  Edit
                </Button>
                <Button
                  size="small"
                  disabled={statusMutation.isPending}
                  onClick={() =>
                    statusMutation.mutate({
                      id: listing.id,
                      status: listing.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
                    })
                  }
                >
                  {listing.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                </Button>
              </CardActions>
            </Card>
          ))}
        </Stack>
      </Box>
    </Container>
  )
}
