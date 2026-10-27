'use client'

import { useEffect, useState } from 'react'

export default function ServiceStatusOverlay() {
  const [isDown, setIsDown] = useState(false)

  const checkStatus = async () => {
    try {
      // 1. Try Next.js internal API proxy
      let res = await fetch('/api/canary-status', { cache: 'no-store' })
      let data: any = null

      if (res.ok) {
        data = await res.json()
      }

      // 2. Direct Spring Boot backend fallback
      if (!data || data.status === 'UNKNOWN' || data.enrollmentOpen === undefined) {
        const directRes = await fetch('https://edustream-0814.onrender.com/api/status', {
          cache: 'no-store',
        })
        if (directRes.ok) {
          data = await directRes.json()
        }
      }

      if (data && data.enrollmentOpen !== undefined) {
        setIsDown(data.enrollmentOpen === false)
      }
    } catch {
      // Network retry
    }
  }

  useEffect(() => {
    checkStatus()
    // Poll every 3 seconds to instantly recover when flag turns ON
    const interval = setInterval(checkStatus, 3000)
    return () => clearInterval(interval)
  }, [])

  // If service is UP, render nothing extra at all (100% clean website)
  if (!isDown) return null

  // If service is DOWN, render the exact pixel-perfect native Next.js 404 page
  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 999999,
        background: '#000',
        color: '#fff',
        fontFamily:
          'system-ui, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji"',
        height: '100vh',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <h1
          style={{
            display: 'inline-block',
            margin: '0 20px 0 0',
            paddingRight: '23px',
            fontSize: '24px',
            fontWeight: 500,
            verticalAlign: 'top',
            borderRight: '1px solid rgba(255, 255, 255, 0.3)',
            lineHeight: '49px',
          }}
        >
          404
        </h1>
        <div style={{ display: 'inline-block', textAlign: 'left', lineHeight: '49px', height: '49px', verticalAlign: 'middle' }}>
          <h2
            style={{
              fontSize: '14px',
              fontWeight: 400,
              lineHeight: '49px',
              margin: 0,
              padding: 0,
              color: '#fff',
            }}
          >
            Cette page est introuvable.
          </h2>
        </div>
      </div>
    </div>
  )
}
