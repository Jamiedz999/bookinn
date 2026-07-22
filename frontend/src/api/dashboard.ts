import { api } from './client'
import type { DashboardSummary, ListingOccupancy, MonthlyRevenue } from './types'

/** Current-month revenue, all-time non-cancelled bookings, and check-ins in the next 7 days. */
export async function getDashboardSummary(): Promise<DashboardSummary> {
  const response = await api.get<DashboardSummary>('/host/dashboard/summary')
  return response.data
}

/** Revenue for the last 12 months, oldest first, with empty months zero-filled. */
export async function getRevenueTrend(): Promise<MonthlyRevenue[]> {
  const response = await api.get<MonthlyRevenue[]>('/host/dashboard/revenue-trend')
  return response.data
}

/** Current-month occupancy per listing, ordered by listing id. */
export async function getOccupancy(): Promise<ListingOccupancy[]> {
  const response = await api.get<ListingOccupancy[]>('/host/dashboard/occupancy')
  return response.data
}
