import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { getDashboardSummary, getOccupancy, getRevenueTrend } from './dashboard'

describe('dashboard api', () => {
  it('getDashboardSummary returns the three KPIs', async () => {
    server.use(
      http.get('/api/host/dashboard/summary', () =>
        HttpResponse.json({
          currentMonthRevenue: 580,
          totalBookings: 7,
          upcomingCheckIns: 2,
        }),
      ),
    )

    const summary = await getDashboardSummary()

    expect(summary.currentMonthRevenue).toBe(580)
    expect(summary.totalBookings).toBe(7)
    expect(summary.upcomingCheckIns).toBe(2)
  })

  it('getRevenueTrend returns the 12-month series', async () => {
    server.use(
      http.get('/api/host/dashboard/revenue-trend', () =>
        HttpResponse.json([
          { month: '2025-08', revenue: 0 },
          { month: '2026-07', revenue: 580 },
        ]),
      ),
    )

    const trend = await getRevenueTrend()

    expect(trend).toHaveLength(2)
    expect(trend[0].month).toBe('2025-08')
    expect(trend[1].revenue).toBe(580)
  })

  it('getOccupancy returns per-listing occupancy', async () => {
    server.use(
      http.get('/api/host/dashboard/occupancy', () =>
        HttpResponse.json([
          { listingId: 1, listingTitle: 'Sea view loft', bookedNights: 7, daysInMonth: 31, rate: 7 / 31 },
        ]),
      ),
    )

    const occupancy = await getOccupancy()

    expect(occupancy).toHaveLength(1)
    expect(occupancy[0].listingTitle).toBe('Sea view loft')
    expect(occupancy[0].bookedNights).toBe(7)
  })
})
