import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { getAmenities } from './amenities'

describe('amenities api', () => {
  it('getAmenities returns the dictionary', async () => {
    server.use(
      http.get('/api/amenities', () =>
        HttpResponse.json([
          { id: 1, name: 'Wifi' },
          { id: 2, name: 'Kitchen' },
        ]),
      ),
    )

    const result = await getAmenities()

    expect(result).toHaveLength(2)
    expect(result[1].name).toBe('Kitchen')
  })
})
