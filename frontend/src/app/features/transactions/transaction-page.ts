import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Category, Transaction } from '../../core/models/api.models';
import { CategoryService } from '../../core/services/category.service';
import { TransactionService } from '../../core/services/transaction.service';

@Component({
  selector: 'app-transactions-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
  ],
  templateUrl: './transaction-page.html',
  styleUrl: './transaction-page.scss',
})
export class TransactionsPage implements OnInit {
  private readonly transactionsApi = inject(TransactionService);
  private readonly categoriesApi = inject(CategoryService);

  readonly filters = new FormGroup({
    from: new FormControl(''),
    to: new FormControl(''),
    categoryId: new FormControl<number | null>(null),
    text: new FormControl(''),
  });

  readonly columns = ['bookingDate', 'description', 'counterparty', 'amount', 'category'];

  transactions: Transaction[] = [];
  categories: Category[] = [];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  sort = 'bookingDate,desc';
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadCategories();
    this.loadTransactions();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.loadTransactions();
  }

  clearFilters(): void {
    this.filters.reset({ from: '', to: '', categoryId: null, text: '' });
    this.pageIndex = 0;
    this.loadTransactions();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadTransactions();
  }

  onSort(event: Sort): void {
    this.sort = event.direction
      ? `${event.active},${event.direction}`
      : 'bookingDate,desc';
    this.pageIndex = 0;
    this.loadTransactions();
  }

  changeCategory(transaction: Transaction, categoryId: number | null): void {
    this.errorMessage = '';

    this.transactionsApi.changeCategory(transaction.id, categoryId).subscribe({
      next: (updated) => {
        this.transactions = this.transactions.map((item) =>
          item.id === updated.id ? updated : item,
        );
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
      },
    });
  }

  private loadCategories(): void {
    this.categoriesApi.list().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
      },
    });
  }

  private loadTransactions(): void {
    const filter = this.filters.getRawValue();
    this.loading = true;
    this.errorMessage = '';

    this.transactionsApi.search({
      from: filter.from || undefined,
      to: filter.to || undefined,
      categoryId: filter.categoryId ?? undefined,
      text: filter.text || undefined,
      page: this.pageIndex,
      size: this.pageSize,
      sort: this.sort,
    }).subscribe({
      next: (page) => {
        this.transactions = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
        this.loading = false;
      },
    });
  }

  private describeError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Cannot reach the backend. Check that it is running.';
      }
      const message = error.error?.message;
      return typeof message === 'string'
        ? message
        : `Request failed (${error.status}).`;
    }
    return 'An unexpected error occurred.';
  }
}