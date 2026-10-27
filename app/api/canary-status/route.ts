import { NextRequest, NextResponse } from 'next/server'

const BACKEND_URL = process.env.BACKEND_URL || 'https://edustream-0814.onrender.com'

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const userId = searchParams.get('userId') || ''
    const url = userId
      ? `${BACKEND_URL}/api/status?userId=${encodeURIComponent(userId)}`
      : `${BACKEND_URL}/api/status`

    const res = await fetch(url, {
      cache: 'no-store',
    })
    const data = await res.json()
    return NextResponse.json(data)
  } catch {
    return NextResponse.json({ status: 'UNKNOWN' })
  }
}
