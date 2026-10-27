'use client'

import { useEffect, useState } from 'react'

interface CanaryMetrics {
  v1UsersCount?: number
  v2UsersCount?: number
  totalUsersCount?: number
  v1Percentage?: number
  v2Percentage?: number
  totalEvaluations?: number
}

interface CanaryStatus {
  service?: string
  status?: string
  canaryActive?: boolean
  assignedVersion?: string
  mode?: string
  userId?: string
  metrics?: CanaryMetrics
}

export default function ServiceStatusOverlay() {
  const [data, setData] = useState<CanaryStatus | null>(null)
  const [collapsed, setCollapsed] = useState(false)
  const [clientId, setClientId] = useState('')

  useEffect(() => {
    // Generate or retrieve persistent machine/client ID for Unleash hashing
    let stored = localStorage.getItem('edustream_canary_client_id')
    if (!stored) {
      stored = 'node_' + Math.random().toString(36).substring(2, 9)
      localStorage.setItem('edustream_canary_client_id', stored)
    }
    setClientId(stored)

    const fetchTelemetry = async () => {
      try {
        let res = await fetch(`/api/canary-status?userId=${encodeURIComponent(stored!)}`, {
          cache: 'no-store',
        })
        if (!res.ok) {
          res = await fetch(`https://edustream-0814.onrender.com/api/status?userId=${encodeURIComponent(stored!)}`, {
            cache: 'no-store',
          })
        }
        if (res.ok) {
          const json = await res.json()
          setData(json)
        }
      } catch {
        // Retry next interval
      }
    }

    fetchTelemetry()
    const interval = setInterval(fetchTelemetry, 3000)
    return () => clearInterval(interval)
  }, [])

  if (!data) return null

  const isCanary = data.canaryActive === true
  const metrics = data.metrics || {}
  const v1Count = metrics.v1UsersCount || 0
  const v2Count = metrics.v2UsersCount || 0
  const total = metrics.totalUsersCount || (v1Count + v2Count)
  const v1Pct = metrics.v1Percentage ?? (total > 0 ? Math.round((v1Count / total) * 100) : 50)
  const v2Pct = metrics.v2Percentage ?? (total > 0 ? Math.round((v2Count / total) * 100) : 50)

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '16px',
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 99999,
        maxWidth: '92vw',
        width: collapsed ? 'auto' : '820px',
        transition: 'all 0.3s ease',
        fontFamily: 'system-ui, -apple-system, Segoe UI, Roboto, sans-serif',
      }}
    >
      <div
        style={{
          background: 'rgba(15, 23, 42, 0.94)',
          border: isCanary ? '1px solid #10b981' : '1px solid #3b82f6',
          borderRadius: '16px',
          boxShadow: isCanary
            ? '0 10px 30px rgba(16, 185, 129, 0.25)'
            : '0 10px 30px rgba(59, 130, 246, 0.25)',
          backdropFilter: 'blur(12px)',
          padding: collapsed ? '8px 16px' : '14px 20px',
          color: '#f8fafc',
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
        }}
      >
        {/* Header row */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
          {/* Node allocation */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span
              style={{
                width: '10px',
                height: '10px',
                borderRadius: '50%',
                background: isCanary ? '#10b981' : '#3b82f6',
                boxShadow: isCanary ? '0 0 10px #10b981' : '0 0 10px #3b82f6',
                display: 'inline-block',
              }}
            />
            <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Your Device is on:</span>
            <span
              style={{
                fontSize: '0.92rem',
                fontWeight: 700,
                color: isCanary ? '#34d399' : '#60a5fa',
                background: isCanary ? 'rgba(16, 185, 129, 0.15)' : 'rgba(59, 130, 246, 0.15)',
                padding: '3px 10px',
                borderRadius: '8px',
                letterSpacing: '0.02em',
              }}
            >
              {isCanary ? '🚀 CANARY v2' : '🛡️ STABLE v1'}
            </span>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>({clientId})</span>
          </div>

          {/* Toggle / Minimize button */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span
              style={{
                fontSize: '0.75rem',
                color: '#cbd5e1',
                background: '#1e293b',
                padding: '3px 8px',
                borderRadius: '6px',
                border: '1px solid #334155',
              }}
            >
              Unleash Flag: <strong>canary-enrollment-v2</strong>
            </span>
            <button
              onClick={() => setCollapsed(!collapsed)}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#94a3b8',
                cursor: 'pointer',
                fontSize: '0.8rem',
                padding: '4px 8px',
              }}
            >
              {collapsed ? '▲ Expand' : '▼ Hide'}
            </button>
          </div>
        </div>

        {/* Expanded body: Live Traffic Distribution */}
        {!collapsed && (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
              borderTop: '1px solid #1e293b',
              paddingTop: '8px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#cbd5e1' }}>
              <span>
                🛡️ <strong>Stable v1 Users:</strong> {v1Count} ({v1Pct}%)
              </span>
              <span>
                🚀 <strong>Canary v2 Users:</strong> {v2Count} ({v2Pct}%)
              </span>
              <span style={{ color: '#64748b' }}>
                Total Connected: <strong>{total}</strong>
              </span>
            </div>

            {/* Visual ratio bar */}
            <div
              style={{
                width: '100%',
                height: '8px',
                background: '#1e293b',
                borderRadius: '999px',
                overflow: 'hidden',
                display: 'flex',
              }}
            >
              <div
                style={{
                  width: `${v1Pct}%`,
                  background: '#3b82f6',
                  transition: 'width 0.4s ease',
                }}
                title={`Stable v1: ${v1Pct}%`}
              />
              <div
                style={{
                  width: `${v2Pct}%`,
                  background: '#10b981',
                  transition: 'width 0.4s ease',
                }}
                title={`Canary v2: ${v2Pct}%`}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
