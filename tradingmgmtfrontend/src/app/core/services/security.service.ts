import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ShareSecurity, ShareSecurityRequest } from '../models/security.model';

@Injectable({ providedIn: 'root' })
export class ShareSecurityService {
  private base = `${environment.apiBaseUrl}/securities`;

  constructor(private http: HttpClient) {}

  list(companyId?: string): Observable<ShareSecurity[]> {
    let params = new HttpParams();
    if (companyId) params = params.set('companyId', companyId);
    return this.http.get<ShareSecurity[]>(this.base, { params });
  }

  get(id: string): Observable<ShareSecurity> {
    return this.http.get<ShareSecurity>(`${this.base}/${id}`);
  }

  create(request: ShareSecurityRequest): Observable<ShareSecurity> {
    return this.http.post<ShareSecurity>(this.base, request);
  }
}
