import { NextResponse } from 'next/server'

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080'

export async function GET() {
  try {
    const res = await fetch(`${BACKEND_URL}/api/status`, {
      cache: 'no-store',
    })
    const data = await res.json()
    return NextResponse.json(data)
  } catch {
    // If backend is unreachable, don't block the site
    return NextResponse.json({ enrollmentOpen: true, status: 'UNKNOWN' })
  }
}
