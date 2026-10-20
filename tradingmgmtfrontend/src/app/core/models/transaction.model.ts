import { Order } from './order.model';
import { ShareSecurity } from './security.model';

export type TransactionStatus =
  | 'DRAFT'
  | 'ORDER_RECEIVED'
  | 'ORDER_VERIFIED'
  | 'ORDER_CONFIRMED'
  | 'PAYMENT_PENDING'
  | 'PAYMENT_RECEIVED'
  | 'PAYMENT_VERIFIED'
  | 'PAYMENT_FAILED'
  | 'TRANSFER_PENDING'
  | 'TRANSFER_INITIATED'
  | 'TRANSFER_COMPLETED'
  | 'TRANSFER_REJECTED'
  | 'SETTLED'
  | 'CANCELLED';

export interface Transaction {
  id: string;
  transactionNumber: string;
  order: Order;
  security: ShareSecurity;
  quantity: number;
  agreedPrice: number;
  totalAmount: number;
  status: TransactionStatus;
  settledAt?: string;
}
