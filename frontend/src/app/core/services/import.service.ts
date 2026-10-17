import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ImportPreview, ImportResult } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/imports';

  preview(accountId: number, file: File): Observable<ImportPreview> {
    const form = new FormData();
    form.append('file', file);

    const params = new HttpParams().set('accountId', accountId);
    return this.http.post<ImportPreview>(this.url, form, { params });
  }

  confirm(batchId: number): Observable<ImportResult> {
    return this.http.post<ImportResult>(`${this.url}/${batchId}/confirm`, {});
  }
}