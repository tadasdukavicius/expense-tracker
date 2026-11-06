import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
  },
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
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' },
];