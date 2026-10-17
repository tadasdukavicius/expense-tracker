import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Category, CategoryRequest } from '../../core/models/api.models';
import { CategoryService } from '../../core/services/category.service';

@Component({
  selector: 'app-category-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './category-page.html',
  styleUrl: './category-page.scss',
})
export class CategoryPage implements OnInit {
  private readonly categoryApi = inject(CategoryService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
    color: new FormControl('#4caf50', {
      nonNullable: true,
      validators: [Validators.pattern(/^#[0-9a-fA-F]{6}$/)],
    }),
  });

  categories: Category[] = [];
  editingId: number | null = null;
  loading = false;
  saving = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadCategories();
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving) return;

    const request: CategoryRequest = {
      name: this.form.controls.name.value.trim(),
      color: this.form.controls.color.value,
    };

    this.saving = true;
    this.errorMessage = '';

    const operation = this.editingId === null
      ? this.categoryApi.create(request)
      : this.categoryApi.update(this.editingId, request);

    operation.subscribe({
      next: () => {
        this.cancelEdit();
        this.saving = false;
        this.loadCategories();
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
        this.saving = false;
      },
    });
  }

  edit(category: Category): void {
    this.editingId = category.id;
    this.form.setValue({
      name: category.name,
      color: category.color ?? '#4caf50',
    });
    this.errorMessage = '';
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form.reset({ name: '', color: '#4caf50' });
    this.errorMessage = '';
  }

  deleteCategory(id: number): void {
    this.errorMessage = '';
    this.categoryApi.delete(id).subscribe({
      next: () => {
        this.categories = this.categories.filter((category) => category.id !== id);
        if (this.editingId === id) this.cancelEdit();
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
      },
    });
  }

  private loadCategories(): void {
    this.loading = true;
    this.categoryApi.list().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error);
        this.loading = false;
        this.cdr.markForCheck();
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