'use client'

import { useEffect, useState } from 'react'

interface CanaryStatus {
  service?: string
  status?: string
  enrollmentOpen?: boolean
  canaryActive?: boolean
  mode?: string
}

export default function ServiceStatusOverlay() {
  const [isDown, setIsDown] = useState(false)
  const [canaryData, setCanaryData] = useState<CanaryStatus | null>(null)
  const [statusText, setStatusText] = useState('Connecting to Unleash...')
  const [lastUpdated, setLastUpdated] = useState<string>('')

  const checkStatus = async () => {
    try {
      // 1. Try Next.js internal API proxy
      let res = await fetch('/api/canary-status', { cache: 'no-store' })
      let data: CanaryStatus | null = null

      if (res.ok) {
        data = await res.json()
      }

      // 2. If status was unknown or failed, try direct Spring Boot backend URL
      if (!data || data.status === 'UNKNOWN' || data.enrollmentOpen === undefined) {
        const directRes = await fetch('https://edustream-0814.onrender.com/api/status', {
          cache: 'no-store',
        })
        if (directRes.ok) {
          data = await directRes.json()
        }
      }

      if (data && data.enrollmentOpen !== undefined) {
        setCanaryData(data)
        const down = data.enrollmentOpen === false
        setIsDown(down)
        setStatusText(down ? 'Rollback Active' : 'v2 Active')
        setLastUpdated(new Date().toLocaleTimeString())
      } else {
        setStatusText('Waking up backend...')
      }
    } catch {
      setStatusText('Reconnecting...')
    }
  }

  useEffect(() => {
    checkStatus()
    // Poll every 4 seconds for instant responsiveness when flipping flags in Unleash
    const interval = setInterval(checkStatus, 4000)
    return () => clearInterval(interval)
  }, [])

  return (
    <>
      {/* 🔴 Full screen overlay when Unleash flag is OFF (Controlled Rollback) */}
      {isDown && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 99999,
            background: 'rgba(10, 5, 5, 0.96)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            padding: '2rem',
            backdropFilter: 'blur(10px)',
          }}
        >
          <div style={{ position: 'relative', marginBottom: '1.5rem' }}>
            <div
              style={{
                width: 90,
                height: 90,
                borderRadius: '50%',
                background: '#dc2626',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '2.5rem',
                boxShadow: '0 0 35px rgba(220, 38, 38, 0.6)',
                animation: 'pulse 1.8s infinite',
              }}
            >
              🔴
            </div>
          </div>

          <h1 style={{ color: '#f87171', fontSize: '2.5rem', fontWeight: 800, margin: '0 0 0.75rem' }}>
            Service Temporarily Unavailable
          </h1>

          <p style={{ color: '#cbd5e1', fontSize: '1.1rem', maxWidth: 520, marginBottom: '1.5rem', lineHeight: 1.6 }}>
            EduStream has been switched to <strong style={{ color: '#f87171' }}>Maintenance / Rollback Mode</strong> via Unleash kill switch.
          </p>

          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.6rem',
              background: '#1a0a0a',
              border: '1px solid #7f1d1d',
              borderRadius: 8,
              padding: '0.6rem 1.4rem',
              marginBottom: '1.5rem',
            }}
          >
            <span
              style={{
                width: 10,
                height: 10,
                borderRadius: '50%',
                background: '#ef4444',
                display: 'inline-block',
                animation: 'pulse 1s infinite',
              }}
            />
            <span style={{ color: '#fca5a5', fontFamily: 'monospace', fontSize: '0.875rem', fontWeight: 600 }}>
              KILL SWITCH ACTIVATED: FLAG canary-enrollment-v2 IS OFF
            </span>
          </div>

          <p style={{ color: '#94a3b8', fontSize: '0.85rem' }}>
            Turn the flag back <strong>ON</strong> in Unleash Cloud to instantly restore access without redeploying.
          </p>
        </div>
      )}

      {/* 🟢 Live floating Canary badge in the bottom-right corner */}
      {!isDown && (
        <div
          style={{
            position: 'fixed',
            bottom: '18px',
            right: '18px',
            zIndex: 99999,
            background: 'rgba(15, 23, 42, 0.95)',
            border: canaryData ? '1px solid #22c55e' : '1px solid #eab308',
            borderRadius: '9999px',
            padding: '8px 16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.6)',
            backdropFilter: 'blur(8px)',
            color: '#f8fafc',
            fontSize: '0.82rem',
            fontWeight: 600,
            fontFamily: 'system-ui, -apple-system, sans-serif',
            cursor: 'pointer',
          }}
          onClick={checkStatus}
          title="Click to manually refresh status"
        >
          <span
            style={{
              width: '9px',
              height: '9px',
              borderRadius: '50%',
              background: canaryData ? '#22c55e' : '#eab308',
              boxShadow: canaryData ? '0 0 10px #22c55e' : '0 0 10px #eab308',
              display: 'inline-block',
              animation: canaryData ? 'none' : 'pulse 1.5s infinite',
            }}
          />
          <span>Unleash Canary: <strong style={{ color: canaryData ? '#4ade80' : '#facc15' }}>{statusText}</strong></span>
          {lastUpdated && <span style={{ color: '#64748b', fontSize: '0.75rem' }}>• {lastUpdated}</span>}
        </div>
      )}

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.65; transform: scale(1.06); }
        }
      `}</style>
    </>
  )
}
