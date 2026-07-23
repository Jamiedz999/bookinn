import { useQuery } from '@tanstack/react-query'
import { Alert, Box, Card, CardContent, Container, Stack, Typography } from '@mui/material'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getDashboardSummary, getOccupancy, getRevenueTrend } from '../api/dashboard'

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function formatPercent(rate: number): string {
  return `${Math.round(rate * 100)}%`
}

/** A single headline number. */
function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <Card variant="outlined" sx={{ flex: 1, minWidth: 160 }}>
      <CardContent>
        <Typography color="text.secondary" variant="body2" gutterBottom>
          {label}
        </Typography>
        <Typography variant="h4">{value}</Typography>
      </CardContent>
    </Card>
  )
}

export function DashboardPage() {
  const summaryQuery = useQuery({ queryKey: ['dashboard-summary'], queryFn: getDashboardSummary })
  const trendQuery = useQuery({ queryKey: ['dashboard-trend'], queryFn: getRevenueTrend })
  const occupancyQuery = useQuery({ queryKey: ['dashboard-occupancy'], queryFn: getOccupancy })

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 6 }}>
        <Typography variant="h4" gutterBottom>
          Dashboard
        </Typography>

        {/* KPI cards */}
        {summaryQuery.isError && <Alert severity="error">Could not load summary.</Alert>}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 4 }}>
          <KpiCard
            label="Revenue this month"
            value={
              summaryQuery.data ? formatCurrency(summaryQuery.data.currentMonthRevenue) : '—'
            }
          />
          <KpiCard
            label="Total bookings"
            value={summaryQuery.data ? String(summaryQuery.data.totalBookings) : '—'}
          />
          <KpiCard
            label="Check-ins next 7 days"
            value={summaryQuery.data ? String(summaryQuery.data.upcomingCheckIns) : '—'}
          />
        </Stack>

        {/* Revenue trend */}
        <Typography variant="h6" gutterBottom>
          Revenue — last 12 months
        </Typography>
        {trendQuery.isError && <Alert severity="error">Could not load revenue trend.</Alert>}
        {trendQuery.data && (
          <Box sx={{ width: '100%', height: 300, mb: 4 }}>
            <ResponsiveContainer>
              <LineChart data={trendQuery.data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                <Line type="monotone" dataKey="revenue" stroke="#1976d2" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </Box>
        )}

        {/* Occupancy */}
        <Typography variant="h6" gutterBottom>
          Occupancy this month
        </Typography>
        {occupancyQuery.isError && <Alert severity="error">Could not load occupancy.</Alert>}
        {occupancyQuery.data?.length === 0 && (
          <Typography color="text.secondary">No listings yet.</Typography>
        )}
        {occupancyQuery.data && occupancyQuery.data.length > 0 && (
          <Box sx={{ width: '100%', height: 60 + occupancyQuery.data.length * 48 }}>
            <ResponsiveContainer>
              <BarChart
                layout="vertical"
                data={occupancyQuery.data}
                margin={{ left: 24, right: 24 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" domain={[0, 1]} tickFormatter={formatPercent} />
                <YAxis type="category" dataKey="listingTitle" width={140} />
                <Tooltip formatter={(value) => formatPercent(Number(value))} />
                <Bar dataKey="rate" fill="#2e7d32" />
              </BarChart>
            </ResponsiveContainer>
          </Box>
        )}
      </Box>
    </Container>
  )
}
