# Product Requirements Document: URL Shortener (Bitly-style)

**Document version:** 1.0  
**Last updated:** September 4, 2026  
**Status:** Draft  
**Source:** [Hello Interview — Design a URL Shortener Like Bitly](https://www.hellointerview.com/learn/system-design/problem-breakdowns/bitly)

---

## 1. Executive Summary

Build a URL shortening service that converts long URLs into compact, shareable links and redirects users to the original destination with minimal latency. The product targets users who need reliable, fast link sharing without account management or analytics in the initial release.

This PRD defines **what** the product must do and **how success is measured**. Technical architecture decisions (hashing vs. counters, caching, sharding) are captured as engineering constraints derived from product requirements.

---

## 2. Problem Statement

Long URLs are difficult to share in messaging apps, social posts, printed materials, and character-limited contexts. Users need a service that:

- Produces short, memorable links on demand
- Reliably redirects anyone who clicks the link
- Supports optional customization and expiration for link lifecycle control

Without a dedicated shortener, users resort to opaque third-party tools with inconsistent uptime, slow redirects, or unwanted tracking—hurting trust and shareability.

---

## 3. Goals & Non-Goals

### Goals

| Goal | Description |
|------|-------------|
| **Fast shortening** | Users receive a short link immediately after submitting a valid long URL |
| **Reliable redirects** | Every valid short link resolves to the correct destination with sub-100ms latency |
| **Global scale** | Support 1B stored URLs and 100M daily active users (redirects) |
| **Uniqueness** | Each short code maps to exactly one long URL at any point in time |

### Non-Goals (v1 — out of scope)

- User authentication and account management
- Click analytics (counts, geography, referrers)
- Real-time analytics consistency
- Spam detection and malicious URL filtering
- Advanced admin or moderation tooling

These may be considered in future phases after core shorten-and-redirect behavior is proven at scale.

---

## 4. Target Users & Personas

### Persona 1: Casual Sharer
Shares links in WhatsApp, Instagram bio, or email. Wants a short link instantly with no signup.

**Needs:** One-click shorten, copy link, share anywhere.

### Persona 2: Campaign Manager (future)
Creates many links for marketing campaigns. May want custom aliases and expiration (supported in v1 as optional fields, without accounts).

**Needs:** Branded or memorable aliases, time-limited links.

### Persona 3: End Recipient (redirect user)
Clicks a short link on mobile or desktop. Expects instant redirect with no visible friction.

**Needs:** Fast, trustworthy redirect; clear error if link expired or invalid.

---

## 5. Functional Requirements

### 5.1 Core Features (P0 — must ship)

#### FR-1: Shorten a URL
**Description:** User submits a long URL and receives a shortened URL on a domain owned by the service (e.g., `short.ly/abc123`).

**Acceptance criteria:**
- Valid HTTP/HTTPS URLs are accepted
- Invalid URL format returns a clear validation error
- Response includes the full short URL ready to copy/share
- Short codes are unique across the system

**Optional inputs (P1 within core flow):**
- **Custom alias:** User-specified path segment (e.g., `short.ly/my-launch`)
- **Expiration date:** After expiry, redirect must not occur; user sees an appropriate error

#### FR-2: Redirect to Original URL
**Description:** Anyone visiting a short URL is redirected to the original long URL.

**Acceptance criteria:**
- Valid, non-expired short codes redirect via HTTP 302 (temporary redirect)
- Expired short codes return HTTP 410 Gone (or equivalent user-facing expired state)
- Unknown short codes return HTTP 404 Not Found
- Redirect is transparent to the user (browser follows automatically)

### 5.2 Feature Details

| ID | Feature | Priority | Notes |
|----|---------|----------|-------|
| FR-1a | URL validation | P0 | Reject malformed URLs before storage |
| FR-1b | Auto-generated short code | P0 | Default when no custom alias provided |
| FR-1c | Custom alias | P1 | Must not collide with existing codes |
| FR-1d | Expiration date | P1 | Enforced on redirect; optional background cleanup |
| FR-2a | 302 redirect | P0 | Preferred over 301 to avoid browser caching and enable future analytics |
| FR-2b | Expired link handling | P1 | 410 + user-friendly message |

### 5.3 Deduplication Policy

**Default:** Allow multiple short codes for the same long URL. Different users may want separate expiration, aliases, or future analytics.

**Optional optimization:** Return existing short code if the exact same long URL was previously shortened (product decision; not required for v1).

---

## 6. Non-Functional Requirements

| ID | Requirement | Target | Rationale |
|----|-------------|--------|-----------|
| NFR-1 | Short code uniqueness | 100% — one code, one URL | Prevents wrong redirects |
| NFR-2 | Redirect latency (p99) | < 100 ms | End-user experience on click |
| NFR-3 | Availability | 99.99% | Redirects are the primary user touchpoint; availability > strong consistency |
| NFR-4 | Scale — stored URLs | 1 billion mappings | Long-term product growth |
| NFR-5 | Scale — daily active users | 100M DAU (redirects) | Read-heavy traffic profile |
| NFR-6 | Read/write ratio | ~1000:1 (reads:writes) | Drives caching and read-path optimization |

### Traffic Estimates (engineering input)

- **Redirects:** ~500M/day average → ~5,800/sec; design for ~600K/sec peak (100× spike factor)
- **New URLs:** ~100K/day → ~1 write/sec average

---

## 7. User Flows

### 7.1 Shorten URL (happy path)

```
User → Enter long URL [+ optional alias, expiration]
     → Submit
     → System validates URL
     → System generates or validates short code
     → System persists mapping
     → User receives short URL (e.g., https://short.ly/abc123)
```

### 7.2 Redirect (happy path)

```
User → Clicks short URL (GET /{short_code})
     → System looks up mapping (cache → database)
     → If found and not expired → HTTP 302 → original URL
     → Browser loads destination
```

### 7.3 Error paths

| Scenario | User experience | HTTP status |
|----------|-----------------|-------------|
| Invalid long URL on create | Inline error: invalid URL | 400 |
| Custom alias taken | Error: alias unavailable | 409 |
| Short code not found | "Link not found" page | 404 |
| Short code expired | "This link has expired" page | 410 |
| Empty / garbled input | Prompt to retry | 400 |

---

## 8. API Specification

### 8.1 Create short URL

```
POST /urls
Content-Type: application/json

Request:
{
  "long_url": "https://www.example.com/some/very/long/url",
  "custom_alias": "optional_custom_alias",      // optional
  "expiration_date": "2026-12-31T23:59:59Z"     // optional, ISO 8601
}

Response 201:
{
  "short_url": "https://short.ly/abc123"
}

Errors: 400 (validation), 409 (alias conflict)
```

### 8.2 Redirect

```
GET /{short_code}

Response 302:
Location: https://www.original-long-url.com

Response 404: short code not found
Response 410: short code expired
```

---

## 9. Data Model (Product View)

| Entity | Description | Key fields |
|--------|-------------|------------|
| **Original URL** | Source link submitted by user | `long_url`, validation metadata |
| **Short URL / mapping** | Short code → long URL relationship | `short_code` (PK), `long_url`, `expiration_date`, `created_at` |
| **User** (future) | Creator of link | Out of scope v1; optional `creator_id` reserved in schema |

**Storage estimate:** ~500 bytes/row × 1B rows ≈ 500 GB — within single-database capacity for v1; sharding deferred until needed.

---

## 10. UX & Content Requirements

### Shortening UI
- Single input for long URL; advanced options (alias, expiration) collapsible or secondary
- Copy-to-clipboard on success
- No login required for v1

### Redirect experience
- No interstitial page on success (direct 302)
- Branded, minimal error pages for 404 and 410

### Tone
- Clear, neutral copy; no promises about matching or outcomes (future product boundary)

---

## 11. Technical Constraints (Product → Engineering)

These constraints ensure NFRs are met without prescribing full architecture:

1. **Short code generation** must guarantee uniqueness (counter + Base62 or hash + collision retry with DB UNIQUE constraint).
2. **Primary key / index** on `short_code` for O(log n) lookups.
3. **In-memory cache** (e.g., Redis) in front of database on read path; cache TTL ≤ URL expiration where applicable.
4. **Separate read and write paths** at scale; horizontal scaling on read tier.
5. **Centralized counter** (Redis INCR or batched ranges) for distributed write instances.
6. **302 redirects** by default; document tradeoff vs. 301 (caching vs. control).
7. **Optional:** CDN / edge redirect for popular codes to reduce origin latency.

---

## 12. Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Redirect latency (p99) | < 100 ms | APM / edge logs |
| Uptime | 99.99% | Synthetic checks + incident tracking |
| Shorten success rate | > 99.9% | API 2xx / total POST |
| Redirect success rate | > 99.99% | 302 / (302 + 5xx) |
| Collision rate on create | ~0 (handled by retry) | DB constraint violations |
| Cache hit rate (redirects) | > 90% at steady state | Cache metrics |

---

## 13. Phased Rollout

### Phase 1 — MVP
- FR-1 (auto-generated codes only)
- FR-2 (302 redirect)
- Single region, database + basic cache
- NFR targets at reduced scale (10M URLs, 1M DAU)

### Phase 2 — Enhanced creation
- Custom alias (FR-1c)
- Expiration date (FR-1d) + 410 handling
- Background job for expired row cleanup

### Phase 3 — Scale & resilience
- Read/write service split
- Redis counter with batching
- DB replication, Redis Sentinel/Cluster
- CDN / edge redirect evaluation

### Phase 4 — Future (out of scope v1)
- Authentication & accounts
- Click analytics dashboard
- Spam / malicious URL filtering
- Multi-region active-active

---

## 14. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Short code collisions | Wrong redirects | UNIQUE DB constraint; retry with salt; prefer counter-based generation |
| Predictable sequential codes | Enumeration / scraping | XOR obfuscation or accept public-share model |
| Cache stale after expiration | Redirect after expiry | TTL aligned with expiration; invalidate on update |
| Redis counter failure | Duplicate codes | UNIQUE constraint as safety net; disjoint regional ranges |
| Traffic spikes on viral links | Slow redirects | CDN edge cache; aggressive read caching |
| Custom alias vs. generated code collision | Create failures | Namespace separation or reserved prefix for generated codes |

---

## 15. Open Questions

1. Should duplicate long URLs return the same short code (dedup) or always create new mappings?
2. Maximum length and character set for custom aliases?
3. Default expiration if none specified — permanent or platform-wide TTL?
4. Branded domains per customer (enterprise) — timeline and pricing model?
5. Legal/compliance: logging of redirect IPs for abuse (conflicts with analytics out-of-scope)?

---

## 16. Appendix

### A. Redirect status code decision

| Code | Behavior | Product implication |
|------|----------|---------------------|
| **301** | Permanent; browsers may cache | Faster repeat visits but loses control if URL changes |
| **302** | Temporary; not cached by default | **Recommended** — supports expiration, updates, future click tracking |

### B. Short code length vs. capacity (Base62)

| Length | Approx. capacity |
|--------|------------------|
| 6 chars | ~56 billion codes |
| 7 chars | ~3.5 trillion codes |
| 8 chars | ~218 trillion codes |

At 1B URLs, 6-character Base62 encoding is sufficient (e.g., `15ftgG`).

### C. References

- [Hello Interview — Bitly System Design Breakdown](https://www.hellointerview.com/learn/system-design/problem-breakdowns/bitly)
- Pattern: Scaling Reads (read-heavy architecture)
