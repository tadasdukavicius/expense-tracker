export interface Category {
  id: number;
  name: string;
  color: string | null;
  emoji: string | null;
}

export interface CategoryRequest {
  name: string;
  color: string | null;
  emoji: string | null;
}

export interface Account {
  id: number;
  name: string;
  currency: string;
}

export interface Transaction {
  id: number;
  bookingDate: string;
  amount: number;
  currency: string;
  description: string;
  counterparty: string | null;
  category: Category | null;
}

export interface TransactionPage {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ImportPreviewRow {
  rowNumber: number;
  bookingDate: string;
  amount: number;
  currency: string;
  description: string;
  counterparty: string | null;
  reference: string | null;
  duplicate: boolean;
}

export interface ImportPreview {
  batchId: number;
  fileName: string;
  totalRows: number;
  newCount: number;
  duplicateCount: number;
  rows: ImportPreviewRow[];
}

export interface ImportResult {
  batchId: number;
  fileName: string;
  status: 'PENDING' | 'CONFIRMED';
  importedCount: number;
  skippedCount: number;
}

export interface AccountRequest {
  name: string;
  currency: string;
}

export interface CategorySpend {
  categoryId: number | null;
  name: string;
  color: string;
  emoji: string | null;
  amount: number;
  percent: number;
}

export interface CategoryStats {
  total: number;
  categories: CategorySpend[];
}
