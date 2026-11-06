import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, ViewChild, inject } from '@angular/core';
import { finalize } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { Account, ImportPreview, ImportResult } from '../../core/models/api.models';
import { AccountService } from '../../core/services/account.service';
import { ImportService } from '../../core/services/import.service';

@Component({
  selector: 'app-import-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatStepperModule,
  ],
  templateUrl: './import-page.html',
  styleUrl: './import-page.scss',
})
export class ImportPage implements OnInit {
  private readonly accountsApi = inject(AccountService);
  private readonly importsApi = inject(ImportService);
  private readonly cdr = inject(ChangeDetectorRef);

  @ViewChild(MatStepper) private stepper?: MatStepper;

  readonly accountForm = new FormGroup({
    accountId: new FormControl<number | null>(null, Validators.required),
  });

  readonly newAccountForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    currency: new FormControl('EUR', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)],
    }),
  });

  accounts: Account[] = [];
  selectedFile: File | null = null;
  preview: ImportPreview | null = null;
  result: ImportResult | null = null;
  loadingAccounts = false;
  creatingAccount = false;
  uploading = false;
  confirming = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadAccounts();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.preview = null;
    this.result = null;
    this.errorMessage = '';
  }

  createAccount(): void {
    this.newAccountForm.markAllAsTouched();
    if (this.newAccountForm.invalid || this.creatingAccount) return;

    const { name, currency } = this.newAccountForm.getRawValue();
    this.creatingAccount = true;
    this.errorMessage = '';

    this.accountsApi.create({ name: name.trim(), currency: currency.toUpperCase() }).pipe(finalize(() => this.cdr.markForCheck())).subscribe({
      next: (account) => {
        this.accounts = [...this.accounts, account];
        this.accountForm.controls.accountId.setValue(account.id);
        this.newAccountForm.reset({ name: '', currency: 'EUR' });
        this.creatingAccount = false;
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
        this.creatingAccount = false;
      },
    });
  }

  uploadForPreview(): void {
    this.accountForm.markAllAsTouched();
    if (this.accountForm.invalid || !this.selectedFile || this.uploading) return;

    this.uploading = true;
    this.errorMessage = '';
    this.preview = null;
    this.result = null;

    this.importsApi.preview(this.accountForm.controls.accountId.value!, this.selectedFile).pipe(finalize(() => this.cdr.markForCheck())).subscribe({
      next: (preview) => {
        this.preview = preview;
        this.uploading = false;
        this.stepper?.next();
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
        this.uploading = false;
      },
    });
  }

  confirmImport(): void {
    if (!this.preview || this.confirming) return;

    this.confirming = true;
    this.errorMessage = '';

    this.importsApi.confirm(this.preview.batchId).pipe(finalize(() => this.cdr.markForCheck())).subscribe({
      next: (result) => {
        this.result = result;
        this.confirming = false;
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
        this.confirming = false;
      },
    });
  }

  startOver(): void {
    this.preview = null;
    this.result = null;
    this.selectedFile = null;
    this.errorMessage = '';
    this.accountForm.controls.accountId.setValue(null);
    this.stepper?.reset();
  }

  private loadAccounts(): void {
    this.loadingAccounts = true;
    this.accountsApi.list().pipe(finalize(() => this.cdr.markForCheck())).subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.loadingAccounts = false;
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
        this.loadingAccounts = false;
      },
    });
  }

  private describeError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) return 'Cannot reach the backend. Check that it is running.';
      const message = error.error?.message;
      return typeof message === 'string' ? message : `Request failed (${error.status}).`;
    }
    return 'An unexpected error occurred.';
  }
}