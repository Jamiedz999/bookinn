import { setupServer } from 'msw/node'

// Empty by default; each test installs the handlers it needs via server.use(...).
export const server = setupServer()
