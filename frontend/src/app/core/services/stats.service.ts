import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CategoryStats } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class StatsService {
  private readonly http = inject(HttpClient);

  byCategory(from?: string, to?: string, accountId?: number): Observable<CategoryStats> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    if (accountId != null) params = params.set('accountId', accountId);
    return this.http.get<CategoryStats>('/api/stats/by-category', { params });
  }
}
