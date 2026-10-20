import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

type Tone = 'pending' | 'progress' | 'good' | 'bad' | 'neutral';

/**
 * Maps every status string used across Order/Transaction/Payment/Transfer
 * to a semantic tone. Falls back to 'neutral' for anything unmapped so a
 * new backend status never renders unstyled.
 */
const TONE_MAP: Record<string, Tone> = {
  DRAFT: 'neutral',
  ORDER_RECEIVED: 'pending',
  ORDER_VERIFIED: 'progress',
  ORDER_CONFIRMED: 'good',
  ORDER_CANCELLED: 'bad',
  ORDER_REJECTED: 'bad',

  PAYMENT_PENDING: 'pending',
  PAYMENT_RECEIVED: 'progress',
  PAYMENT_VERIFIED: 'good',
  PAYMENT_FAILED: 'bad',
  PAYMENT_CONFIRMED: 'good',
  PAYMENT_CANCELLED: 'bad',
  CHEQUE_RECEIVED: 'pending',
  CHEQUE_DEPOSITED: 'progress',
  CHEQUE_CLEARED: 'progress',

  TRANSFER_PENDING: 'pending',
  DOCUMENTS_PENDING: 'pending',
  DOCUMENTS_VERIFIED: 'progress',
  TRANSFER_INITIATED: 'progress',
  TRANSFER_SUBMITTED: 'progress',
  TRANSFER_IN_PROCESS: 'progress',
  TRANSFER_COMPLETED: 'good',
  TRANSFER_REJECTED: 'bad',

  SETTLED: 'good',
  CANCELLED: 'bad',

  PENDING: 'pending',
  VERIFIED: 'good',
  REJECTED: 'bad',
};

const LABEL_OVERRIDES: Record<string, string> = {
  ORDER_RECEIVED: 'Received',
  ORDER_VERIFIED: 'Verified',
  ORDER_CONFIRMED: 'Confirmed',
  ORDER_CANCELLED: 'Cancelled',
  ORDER_REJECTED: 'Rejected',
  PAYMENT_PENDING: 'Payment pending',
  PAYMENT_RECEIVED: 'Payment received',
  PAYMENT_VERIFIED: 'Payment verified',
  PAYMENT_FAILED: 'Payment failed',
  PAYMENT_CONFIRMED: 'Confirmed',
  CHEQUE_RECEIVED: 'Cheque received',
  CHEQUE_DEPOSITED: 'Cheque deposited',
  CHEQUE_CLEARED: 'Cheque cleared',
  TRANSFER_PENDING: 'Transfer pending',
  TRANSFER_INITIATED: 'Transfer initiated',
  TRANSFER_SUBMITTED: 'Transfer submitted',
  TRANSFER_IN_PROCESS: 'Transfer in process',
  TRANSFER_COMPLETED: 'Transfer completed',
  TRANSFER_REJECTED: 'Transfer rejected',
  SETTLED: 'Settled',
  CANCELLED: 'Cancelled',
  DRAFT: 'Draft',
  PENDING: 'Pending review',
  VERIFIED: 'Verified',
  REJECTED: 'Rejected',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [ngStyle]="style">{{ label }}</span>`,
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;

  get tone(): Tone {
    return TONE_MAP[this.status] ?? 'neutral';
  }

  get label(): string {
    return LABEL_OVERRIDES[this.status] ?? this.status;
  }

  get style(): Record<string, string> {
    return {
      color: `var(--status-${this.tone}-fg)`,
      background: `var(--status-${this.tone}-bg)`,
    };
  }
}
