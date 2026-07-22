import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { DashboardPage } from './DashboardPage'

// Recharts' ResponsiveContainer measures its parent, which is 0×0 in jsdom; stub it with a
// fixed-size box so the charts mount without warnings. We assert on the KPIs and headings, which
// live outside the SVG, so chart internals don't need to render.
vi.mock('recharts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('recharts')>()
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: ReactNode }) => (
      <div style={{ width: 800, height: 400 }}>{children}</div>
    ),
  }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <DashboardPage />
    </QueryClientProvider>,
  )
}

const summary = { currentMonthRevenue: 580, totalBookings: 7, upcomingCheckIns: 2 }
const trend = [
  { month: '2025-08', revenue: 0 },
  { month: '2026-07', revenue: 580 },
]
const occupancy = [
  { listingId: 1, listingTitle: 'Sea view loft', bookedNights: 7, daysInMonth: 31, rate: 7 / 31 },
]

function installHandlers(occupancyData: typeof occupancy = occupancy) {
  server.use(
    http.get('/api/host/dashboard/summary', () => HttpResponse.json(summary)),
    http.get('/api/host/dashboard/revenue-trend', () => HttpResponse.json(trend)),
    http.get('/api/host/dashboard/occupancy', () => HttpResponse.json(occupancyData)),
  )
}

describe('DashboardPage', () => {
  it('renders the three KPI cards and both chart sections', async () => {
    installHandlers()

    renderPage()

    expect(await screen.findByText('$580.00')).toBeInTheDocument()
    expect(screen.getByText('Revenue this month')).toBeInTheDocument()
    expect(screen.getByText('Total bookings')).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
    expect(screen.getByText('Check-ins next 7 days')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('Revenue — last 12 months')).toBeInTheDocument()
    expect(screen.getByText('Occupancy this month')).toBeInTheDocument()
  })

  it('shows an empty state when the host has no listings', async () => {
    installHandlers([])

    renderPage()

    expect(await screen.findByText(/no listings yet/i)).toBeInTheDocument()
  })

  it('shows an error when the summary fails to load', async () => {
    server.use(
      http.get('/api/host/dashboard/summary', () => new HttpResponse(null, { status: 500 })),
      http.get('/api/host/dashboard/revenue-trend', () => HttpResponse.json(trend)),
      http.get('/api/host/dashboard/occupancy', () => HttpResponse.json(occupancy)),
    )

    renderPage()

    expect(await screen.findByText(/could not load summary/i)).toBeInTheDocument()
  })
})
