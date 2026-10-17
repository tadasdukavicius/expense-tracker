import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'transactions',
    loadComponent: () =>
      import('./features/transactions/transaction-page').then((m) => m.TransactionsPage),
  },
  {
    path: 'categories',
    loadComponent: () =>
        import('./features/categories/category-page').then((m) => m.CategoryPage),
  },
  {
    path: 'import',
    loadComponent: () =>
        import('./features/import/import-page').then((m) => m.ImportPage),
  },
  { path: '', pathMatch: 'full', redirectTo: 'transactions' },
  { path: '**', redirectTo: 'transactions' },
];