import { Customer } from './customer.model';
import { ShareSecurity } from './security.model';

export type OrderType = 'BUY' | 'SELL';
export type OrderStatus =
  | 'DRAFT'
  | 'ORDER_RECEIVED'
  | 'ORDER_VERIFIED'
  | 'ORDER_CONFIRMED'
  | 'ORDER_CANCELLED'
  | 'ORDER_REJECTED';

export interface Order {
  id: string;
  orderNumber: string;
  orderType: OrderType;
  buyerCustomer?: Customer;
  sellerCustomer?: Customer;
  security: ShareSecurity;
  quantity: number;
  pricePerShare: number;
  grossAmount: number;
  currency: string;
  orderDate: string;
  status: OrderStatus;
  notes?: string;
}

export interface OrderCreateRequest {
  orderType: OrderType;
  buyerCustomerId?: string;
  sellerCustomerId?: string;
  securityId: string;
  quantity: number;
  pricePerShare: number;
  orderDate?: string;
  notes?: string;
}
