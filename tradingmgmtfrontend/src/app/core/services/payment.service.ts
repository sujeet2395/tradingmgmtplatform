import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChequeReceivedRequest,
  Payment,
  PaymentInitiateRequest,
} from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private txBase = `${environment.apiBaseUrl}/transactions`;
  private base = `${environment.apiBaseUrl}/payments`;

  constructor(private http: HttpClient) {}

  listByTransaction(transactionId: string): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.txBase}/${transactionId}/payments`);
  }

  initiate(transactionId: string, request: PaymentInitiateRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.txBase}/${transactionId}/payment`, request);
  }

  chequeReceived(paymentId: string, request: ChequeReceivedRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.base}/${paymentId}/cheque-received`, request);
  }

  deposit(paymentId: string, depositDate?: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.base}/${paymentId}/deposit`, { depositDate });
  }

  verify(paymentId: string, confirmationDate?: string, reason?: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.base}/${paymentId}/verify`, { confirmationDate, reason });
  }
}
