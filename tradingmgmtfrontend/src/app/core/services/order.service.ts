import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Order, OrderCreateRequest, OrderStatus } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private base = `${environment.apiBaseUrl}/orders`;

  constructor(private http: HttpClient) {}

  list(status?: OrderStatus): Observable<Order[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Order[]>(this.base, { params });
  }

  get(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.base}/${id}`);
  }

  create(request: OrderCreateRequest): Observable<Order> {
    return this.http.post<Order>(this.base, request);
  }

  verify(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.base}/${id}/verify`, {});
  }

  confirm(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.base}/${id}/confirm`, {});
  }

  reject(id: string, reason: string): Observable<Order> {
    return this.http.post<Order>(`${this.base}/${id}/reject`, { reason });
  }
}
