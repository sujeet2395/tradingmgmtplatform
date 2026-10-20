import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Company, CompanyRequest } from '../models/company.model';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private base = `${environment.apiBaseUrl}/companies`;

  constructor(private http: HttpClient) {}

  list(): Observable<Company[]> {
    return this.http.get<Company[]>(this.base);
  }

  get(id: string): Observable<Company> {
    return this.http.get<Company>(`${this.base}/${id}`);
  }

  create(request: CompanyRequest): Observable<Company> {
    return this.http.post<Company>(this.base, request);
  }
}
