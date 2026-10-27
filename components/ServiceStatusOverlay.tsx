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
  const [pulse, setPulse] = useState(false)

  useEffect(() => {
    let stored = localStorage.getItem('edustream_canary_client_id')
    if (!stored) {
      stored = 'node_' + Math.random().toString(36).substring(2, 8)
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
          setPulse(prev => !prev)
        }
      } catch {
        // Retry
      }
    }

    fetchTelemetry()
    // ⚡ Fast live polling (every 1.5 seconds) - zero refresh needed!
    const interval = setInterval(fetchTelemetry, 1500)

    // Re-check immediately when switching back to tab or VMware window
    window.addEventListener('focus', fetchTelemetry)

    return () => {
      clearInterval(interval)
      window.removeEventListener('focus', fetchTelemetry)
    }
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
        bottom: '18px',
        right: '18px',
        zIndex: 99999,
        fontFamily: 'system-ui, -apple-system, Segoe UI, Roboto, sans-serif',
      }}
    >
      <div
        style={{
          background: 'rgba(15, 23, 42, 0.96)',
          border: isCanary ? '1px solid #10b981' : '1px solid #3b82f6',
          borderRadius: '12px',
          boxShadow: isCanary
            ? '0 6px 22px rgba(16, 185, 129, 0.3)'
            : '0 6px 22px rgba(59, 130, 246, 0.3)',
          backdropFilter: 'blur(12px)',
          padding: collapsed ? '6px 12px' : '10px 14px',
          color: '#f8fafc',
          width: collapsed ? 'auto' : '265px',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        }}
      >
        {/* Header line */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: isCanary ? '#10b981' : '#3b82f6',
                boxShadow: isCanary ? '0 0 10px #10b981' : '0 0 10px #3b82f6',
                display: 'inline-block',
                transition: 'background 0.3s ease',
              }}
            />
            <span
              style={{
                fontSize: '0.82rem',
                fontWeight: 700,
                color: isCanary ? '#34d399' : '#60a5fa',
                transition: 'color 0.3s ease',
              }}
            >
              {isCanary ? '🚀 CANARY v2' : '🛡️ STABLE v1'}
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            {/* Live pulsating heartbeat dot */}
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '3px',
                fontSize: '0.65rem',
                color: '#10b981',
                background: 'rgba(16, 185, 129, 0.1)',
                padding: '2px 5px',
                borderRadius: '4px',
              }}
            >
              <span
                style={{
                  width: '5px',
                  height: '5px',
                  borderRadius: '50%',
                  background: '#10b981',
                  opacity: pulse ? 1 : 0.4,
                  transition: 'opacity 0.4s ease',
                }}
              />
              LIVE
            </span>

            <button
              onClick={() => setCollapsed(!collapsed)}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#64748b',
                cursor: 'pointer',
                fontSize: '0.75rem',
                padding: '2px 4px',
              }}
              title={collapsed ? 'Expand' : 'Minimize'}
            >
              {collapsed ? '▲' : '▼'}
            </button>
          </div>
        </div>

        {/* Compact stats body */}
        {!collapsed && (
          <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px solid #1e293b' }}>
            {/* Live split numbers */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '0.75rem',
                color: '#cbd5e1',
                marginBottom: '6px',
              }}
            >
              <span>
                🛡️ v1: <strong style={{ color: '#60a5fa' }}>{v1Count}</strong>
              </span>
              <span>
                🚀 v2: <strong style={{ color: '#34d399' }}>{v2Count}</strong>
              </span>
              <span style={{ color: '#64748b' }}>
                Total: <strong>{total}</strong>
              </span>
            </div>

            {/* Slim ratio bar */}
            <div
              style={{
                width: '100%',
                height: '5px',
                background: '#1e293b',
                borderRadius: '999px',
                overflow: 'hidden',
                display: 'flex',
                marginBottom: '6px',
              }}
            >
              <div style={{ width: `${v1Pct}%`, background: '#3b82f6', transition: 'width 0.4s ease' }} />
              <div style={{ width: `${v2Pct}%`, background: '#10b981', transition: 'width 0.4s ease' }} />
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.68rem', color: '#64748b' }}>
              <span>Unleash 50% split</span>
              <span style={{ fontFamily: 'monospace' }}>{clientId}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
