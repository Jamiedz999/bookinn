import { type FormEvent, useEffect, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Container,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { createListing, getHostListing, updateListing } from '../api/listings'
import { getAmenities } from '../api/amenities'
import type { Amenity, ListingInput } from '../api/types'
import { useAuth } from '../auth/useAuth'

export function ListingFormPage() {
  const { user } = useAuth()
  const isHost = user?.roles.includes('HOST') ?? false
  const { id } = useParams<{ id: string }>()
  const listingId = id ? Number(id) : null
  const isEdit = listingId != null
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [city, setCity] = useState('')
  const [address, setAddress] = useState('')
  const [pricePerNight, setPricePerNight] = useState('')
  const [maxGuests, setMaxGuests] = useState('1')
  const [amenities, setAmenities] = useState<Amenity[]>([])
  const [photoUrls, setPhotoUrls] = useState<string[]>([''])

  const amenitiesQuery = useQuery({ queryKey: ['amenities'], queryFn: getAmenities })

  const listingQuery = useQuery({
    queryKey: ['host-listing', listingId],
    queryFn: () => getHostListing(listingId as number),
    enabled: isEdit && Number.isFinite(listingId),
  })

  useEffect(() => {
    const listing = listingQuery.data
    if (!listing) {
      return
    }
    setTitle(listing.title)
    setDescription(listing.description ?? '')
    setCity(listing.city)
    setAddress(listing.address)
    setPricePerNight(String(listing.pricePerNight))
    setMaxGuests(String(listing.maxGuests))
    setAmenities(listing.amenities)
    setPhotoUrls(listing.photoUrls.length > 0 ? listing.photoUrls : [''])
  }, [listingQuery.data])

  const mutation = useMutation({
    mutationFn: (body: ListingInput) =>
      isEdit ? updateListing(listingId as number, body) : createListing(body),
    onSuccess: () => navigate('/host/listings'),
  })

  if (!isHost) {
    return <Navigate to="/host/listings" replace />
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate({
      title,
      description,
      city,
      address,
      pricePerNight: Number(pricePerNight),
      maxGuests: Number(maxGuests),
      amenityIds: amenities.map((amenity) => amenity.id),
      photoUrls: photoUrls.map((url) => url.trim()).filter((url) => url.length > 0),
    })
  }

  function updatePhoto(index: number, value: string) {
    setPhotoUrls((urls) => urls.map((url, position) => (position === index ? value : url)))
  }

  return (
    <Container maxWidth="sm">
      <Box component="form" onSubmit={handleSubmit} sx={{ mt: 6 }}>
        <Typography variant="h4" gutterBottom>
          {isEdit ? 'Edit listing' : 'New listing'}
        </Typography>
        {isEdit && listingQuery.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Could not load this listing.
          </Alert>
        )}
        {mutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Could not save the listing. Please check the fields and try again.
          </Alert>
        )}
        <Stack spacing={2}>
          <TextField label="Title" value={title} onChange={(e) => setTitle(e.target.value)} required />
          <TextField
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            multiline
            minRows={3}
          />
          <TextField label="City" value={city} onChange={(e) => setCity(e.target.value)} required />
          <TextField
            label="Address"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            required
          />
          <TextField
            label="Price per night"
            type="number"
            value={pricePerNight}
            onChange={(e) => setPricePerNight(e.target.value)}
            required
            slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
          />
          <TextField
            label="Max guests"
            type="number"
            value={maxGuests}
            onChange={(e) => setMaxGuests(e.target.value)}
            required
            slotProps={{ htmlInput: { min: 1, step: 1 } }}
          />

          <Autocomplete
            multiple
            options={amenitiesQuery.data ?? []}
            getOptionLabel={(option) => option.name}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            value={amenities}
            onChange={(_, value) => setAmenities(value)}
            renderInput={(params) => <TextField {...params} label="Amenities" />}
          />

          <Box>
            <Typography variant="subtitle1" gutterBottom>
              Photos
            </Typography>
            <Stack spacing={1}>
              {photoUrls.map((url, index) => (
                <Stack key={index} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <TextField
                    fullWidth
                    label={`Photo URL ${index + 1}`}
                    value={url}
                    onChange={(e) => updatePhoto(index, e.target.value)}
                  />
                  <Button
                    onClick={() => setPhotoUrls((urls) => urls.filter((_, i) => i !== index))}
                  >
                    Remove
                  </Button>
                </Stack>
              ))}
            </Stack>
            <Button onClick={() => setPhotoUrls((urls) => [...urls, ''])} sx={{ mt: 1 }}>
              Add photo URL
            </Button>
          </Box>

          <Button type="submit" variant="contained" disabled={mutation.isPending}>
            {isEdit ? 'Save changes' : 'Create listing'}
          </Button>
        </Stack>
      </Box>
    </Container>
  )
}
