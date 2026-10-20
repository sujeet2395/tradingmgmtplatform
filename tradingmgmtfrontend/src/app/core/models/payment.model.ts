export type PaymentMethod = 'CHEQUE' | 'BANK_TRANSFER' | 'OTHER';
export type PaymentStatus =
  | 'PAYMENT_PENDING'
  | 'CHEQUE_RECEIVED'
  | 'CHEQUE_DEPOSITED'
  | 'CHEQUE_CLEARED'
  | 'PAYMENT_CONFIRMED'
  | 'PAYMENT_FAILED'
  | 'PAYMENT_CANCELLED';

export interface Payment {
  id: string;
  amount: number;
  paymentMethod: PaymentMethod;
  chequeNumber?: string;
  bankName?: string;
  chequeDate?: string;
  depositDate?: string;
  clearanceDate?: string;
  confirmationDate?: string;
  status: PaymentStatus;
}

export interface PaymentInitiateRequest {
  amount: number;
  paymentMethod?: PaymentMethod;
  idempotencyKey?: string;
}

export interface ChequeReceivedRequest {
  chequeNumber: string;
  bankName?: string;
  chequeDate?: string;
}
