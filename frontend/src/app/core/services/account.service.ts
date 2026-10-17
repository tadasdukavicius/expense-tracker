import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Account, AccountRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/accounts';

  list(): Observable<Account[]> {
    return this.http.get<Account[]>(this.url);
  }

  create(request: AccountRequest): Observable<Account> {
    return this.http.post<Account>(this.url, request);
  }
}