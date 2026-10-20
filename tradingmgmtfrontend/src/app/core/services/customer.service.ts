import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Customer, CustomerRequest } from '../models/customer.model';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private base = `${environment.apiBaseUrl}/customers`;

  constructor(private http: HttpClient) {}

  list(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.base);
  }

  get(id: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.base}/${id}`);
  }

  create(request: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.base, request);
  }

  verifyKyc(id: string) {
    return this.http.post<Customer>(`${this.base}/${id}/kyc/verify`, {});
  }
  revokeKyc(id: string) {
    return this.http.post<Customer>(`${this.base}/${id}/kyc/revoke`, {});
  }
}
