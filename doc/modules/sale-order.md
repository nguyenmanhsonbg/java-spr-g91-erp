# Sale Order Module

## Summary

This project does not store Sale Order in a separate table. For MVP, a submitted contract becomes the executable Sale Order source.

- Commercial setup remains in `contract`
- Fulfillment tracking is exposed through `saleorder`
- Inventory issue links through `inventory_transactions.related_order_id`
- Invoice, payment, and debt remain downstream of invoice

## Main Entities

- `contracts`
  - Added `sale_order_number`
  - Added `actual_delivery_date`
- `contract_items`
  - Added `reserved_quantity`
  - Added `issued_quantity`
  - Added `delivered_quantity`
  - Added `fulfillment_note`
- `contract_status_history`
  - Reused for status audit
- `contract_tracking_events`
  - Reused for timeline
- `inventory_transactions`
  - Reused `related_order_id` for inventory issue traceability
- `invoices`
  - Reused `contract_id` as sale order source reference

## Status Flow

Contract pre-submit remains in contract APIs:

- `DRAFT`
- `PENDING_APPROVAL`

Executable Sale Order flow starts after submit:

- `SUBMITTED`
- `PROCESSING`
- `RESERVED`
- `PICKED`
- `IN_TRANSIT`
- `DELIVERED`
- `COMPLETED`
- `CANCELLED`

Transition rules implemented:

- only submitted/executable contracts are visible as sale orders
- terminal states `COMPLETED` and `CANCELLED` cannot continue fulfillment
- `RESERVED` requires inventory availability
- `PICKED`, `IN_TRANSIT`, and `DELIVERED` require all items fully issued
- `COMPLETED` requires all items fully delivered
- cancel is delegated to existing contract cancellation flow

## APIs

- `GET /api/sale-orders`
- `GET /api/sale-orders/{id}`
- `GET /api/sale-orders/{id}/timeline`
- `PATCH /api/sale-orders/{id}/status`
- `POST /api/sale-orders/{id}/reserve`
- `POST /api/sale-orders/{id}/pick`
- `POST /api/sale-orders/{id}/dispatch`
- `POST /api/sale-orders/{id}/deliver`
- `POST /api/sale-orders/{id}/complete`
- `POST /api/sale-orders/{id}/cancel`
- `POST /api/sale-orders/{id}/invoices`
- existing source endpoint: `POST /api/contracts/{id}/submit`

Related updates:

- `GET /api/inventory/history` now supports `relatedOrderId`
- `POST /api/inventory/issues` now requires `relatedOrderId` or `relatedProjectId`

## Inventory Integration

- inventory issue can register against `relatedOrderId`
- issue quantity updates `contract_items.issued_quantity`
- issue quantity cannot exceed ordered quantity
- first issue on `SUBMITTED` order auto-moves order to `PROCESSING`
- sale order detail includes related inventory issue history

## Financial Integration

- invoice creation from sale order delegates to the payment module
- invoice eligibility is limited to sale orders in `DELIVERED` or `COMPLETED`
- debt still belongs to invoice/payment flow, not directly to sale order
- traceability chain is:
  - sale order -> invoice -> payment allocation -> debt settlement

## Authorization

- `CUSTOMER`
  - view only own sale orders
- `WAREHOUSE`
  - view sale orders
  - update warehouse fulfillment states
  - create inventory issue linked to sale order
- `ACCOUNTANT`
  - view sale orders
  - submit contracts
  - complete or cancel sale orders
  - create invoice from delivered sale order
- `OWNER`
  - full visibility
  - approve contracts
  - perform all sale order actions

## Assumptions

- no separate frontend/theme layer exists in this repository, so only backend APIs and response contracts were implemented
- pre-submit lifecycle stays in contract module instead of duplicating draft approval logic in sale order module
- completion is fulfillment-based; no extra project financial gate was added because current codebase does not expose a stable approval rule for that step
- cancel continues to use existing contract cancellation rules, including approval behavior where applicable
