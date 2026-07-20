import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Chip,
  Container,
  ImageList,
  ImageListItem,
  Stack,
  Typography,
} from '@mui/material'
import { getListing } from '../api/listings'

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
      </Box>
    </Container>
  )
}
