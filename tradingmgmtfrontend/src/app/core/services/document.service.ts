import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AppDocument, DocumentCategory } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private base = `${environment.apiBaseUrl}/documents`;
  constructor(private http: HttpClient) {}

  list(type: string, id: string) {
    return this.http.get<AppDocument[]>(this.base, {
      params: { relatedEntityType: type, relatedEntityId: id },
    });
  }

  upload(file: File, category: DocumentCategory, type: string, id: string) {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('category', category);
    fd.append('relatedEntityType', type);
    fd.append('relatedEntityId', id);
    return this.http.post<AppDocument>(`${this.base}/upload`, fd);
  }

  verify(id: string) {
    return this.http.post<AppDocument>(`${this.base}/${id}/verify`, {});
  }

  reject(id: string, note: string) {
    return this.http.post<AppDocument>(`${this.base}/${id}/reject`, { note });
  }

  downloadUrl(id: string) {
    return `${this.base}/${id}/download`;
  }
}
