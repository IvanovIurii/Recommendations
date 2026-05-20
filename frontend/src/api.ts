const API_BASE = 'http://localhost:8080/api/v1'

export async function createRfq(data: {
  email: string
  fullName: string
  countryCode: string
  title: string
  description: string
  deliveryLocation: string
  quantity: string
  supplierTypes: string[]
  buyerCountry: string
  categoryId: number
}) {
  const res = await fetch(`${API_BASE}/rfq`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`Create RFQ failed: ${res.status}`)
  return res.json()
}

export async function acceptRfq(rfqId: string) {
  const res = await fetch(`${API_BASE}/rfq/${rfqId}/accept`, { method: 'POST' })
  if (!res.ok) throw new Error(`Accept RFQ failed: ${res.status}`)
}

export async function getRfq(rfqId: string) {
  const res = await fetch(`${API_BASE}/rfq/${rfqId}`)
  if (!res.ok) throw new Error(`Get RFQ failed: ${res.status}`)
  return res.json()
}

export async function getRecommendations(rfqId: string) {
  const res = await fetch(`${API_BASE}/rfq/${rfqId}/recommendations?page=0&pageSize=20`)
  if (!res.ok) throw new Error(`Get recommendations failed: ${res.status}`)
  return res.json()
}

export async function selectSupplier(rfqId: string, supplierId: string, reason?: string) {
  const res = await fetch(`${API_BASE}/rfq/${rfqId}/recommendations/${supplierId}/select`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
  if (!res.ok) throw new Error(`Select supplier failed: ${res.status}`)
}

export async function getPipelineEvents(rfqId: string, since = 0) {
  const res = await fetch(`${API_BASE}/online-pipeline/${rfqId}/events?since=${since}`)
  if (!res.ok) return []
  return res.json()
}

export async function getPipelineRuns() {
  const res = await fetch(`${API_BASE}/pipeline/runs`)
  if (!res.ok) return []
  return res.json()
}
