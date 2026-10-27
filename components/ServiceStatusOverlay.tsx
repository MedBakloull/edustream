'use client'

import { useEffect, useState } from 'react'

export default function ServiceStatusOverlay() {
  const [isDown, setIsDown] = useState(false)
  const [checking, setChecking] = useState(false)

  const checkStatus = async () => {
    try {
      const res = await fetch('/api/canary-status', { cache: 'no-store' })
      const data = await res.json()
      setIsDown(data.enrollmentOpen === false)
    } catch {
      // Can't reach backend — don't block the site
      setIsDown(false)
    }
  }

  useEffect(() => {
    checkStatus()
    // Poll every 8 seconds — changes in Unleash appear automatically
    const interval = setInterval(checkStatus, 8000)
    return () => clearInterval(interval)
  }, [])

  if (!isDown) return null

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        background: 'rgba(10, 5, 5, 0.97)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '2rem',
        backdropFilter: 'blur(8px)',
      }}
    >
      {/* Pulsing red circle */}
      <div style={{ position: 'relative', marginBottom: '2rem' }}>
        <div
          style={{
            width: 80,
            height: 80,
            borderRadius: '50%',
            background: '#dc2626',
            animation: 'pulse 2s infinite',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '2rem',
          }}
        >
          🔴
        </div>
      </div>

      <h1 style={{ color: '#f87171', fontSize: '2.5rem', fontWeight: 800, margin: '0 0 1rem' }}>
        Service Unavailable
      </h1>

      <p style={{ color: '#cbd5e1', fontSize: '1.1rem', maxWidth: 480, marginBottom: '1.5rem', lineHeight: 1.6 }}>
        EduStream is currently undergoing a <strong style={{ color: '#f87171' }}>controlled rollback</strong>.
        Our engineering team has temporarily disabled the service.
      </p>

      <div
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.5rem',
          background: '#1a0a0a',
          border: '1px solid #7f1d1d',
          borderRadius: 8,
          padding: '0.6rem 1.2rem',
          marginBottom: '2rem',
        }}
      >
        <span
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            background: '#ef4444',
            display: 'inline-block',
            animation: 'pulse 1.5s infinite',
          }}
        />
        <span style={{ color: '#fca5a5', fontFamily: 'monospace', fontSize: '0.875rem' }}>
          STATUS: CANARY ROLLBACK IN PROGRESS
        </span>
      </div>

      <p style={{ color: '#64748b', fontSize: '0.8rem' }}>
        This page will automatically recover when the service is restored.
      </p>

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.7; transform: scale(1.05); }
        }
      `}</style>
    </div>
  )
}
