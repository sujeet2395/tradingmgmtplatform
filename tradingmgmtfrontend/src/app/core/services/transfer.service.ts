import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ShareTransfer } from '../models/transfer.model';

@Injectable({ providedIn: 'root' })
export class TransferService {
  private txBase = `${environment.apiBaseUrl}/transactions`;
  private base = `${environment.apiBaseUrl}/transfers`;

  constructor(private http: HttpClient) {}

  listByTransaction(transactionId: string): Observable<ShareTransfer[]> {
    return this.http.get<ShareTransfer[]>(`${this.txBase}/${transactionId}/transfers`);
  }

  initiate(transactionId: string): Observable<ShareTransfer> {
    return this.http.post<ShareTransfer>(`${this.txBase}/${transactionId}/transfer`, {});
  }

  submit(transferId: string, note?: string): Observable<ShareTransfer> {
    return this.http.post<ShareTransfer>(`${this.base}/${transferId}/submit`, { note });
  }

  complete(transferId: string, completedDate?: string): Observable<ShareTransfer> {
    return this.http.post<ShareTransfer>(`${this.base}/${transferId}/complete`, { completedDate });
  }

  reject(transferId: string, reason: string): Observable<ShareTransfer> {
    return this.http.post<ShareTransfer>(`${this.base}/${transferId}/reject`, { reason });
  }
}
