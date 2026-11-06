import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Transaction, TransactionPage } from '../models/api.models';

export interface TransactionQuery {
  from?: string;
  to?: string;
  categoryId?: number;
  accountId?: number;
  text?: string;
  page: number;
  size: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/transactions';

  search(query: TransactionQuery): Observable<TransactionPage> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sort', query.sort ?? 'bookingDate,desc');

    if (query.from) params = params.set('from', query.from);
    if (query.to) params = params.set('to', query.to);
    if (query.categoryId != null) params = params.set('categoryId', query.categoryId);
    if (query.accountId != null) params = params.set('accountId', query.accountId);
    if (query.text?.trim()) params = params.set('q', query.text.trim());

    return this.http.get<TransactionPage>(this.url, { params });
  }

  changeCategory(id: number, categoryId: number | null): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.url}/${id}`, { categoryId });
  }
}