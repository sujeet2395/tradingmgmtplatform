import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Transaction } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private base = `${environment.apiBaseUrl}/transactions`;

  constructor(private http: HttpClient) {}

  get(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.base}/${id}`);
  }

  getByOrder(orderId: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.base}/by-order/${orderId}`);
  }
}
