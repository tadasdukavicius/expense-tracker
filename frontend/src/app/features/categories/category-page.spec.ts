import { of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { CategoryService } from '../../core/services/category.service';
import { CategoryPage } from './category-page';
import { vi } from 'vitest';

describe('CategoryPage', () => {
  const groceries = { id: 1, name: 'Groceries', color: '#4caf50', emoji: null };
  let categoryApi: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    categoryApi = {
      list: vi.fn().mockReturnValue(of([groceries])),
      create: vi.fn().mockReturnValue(of(groceries)),
      update: vi.fn().mockReturnValue(of(groceries)),
      delete: vi.fn().mockReturnValue(of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [CategoryPage],
      providers: [{ provide: CategoryService, useValue: categoryApi }],
    }).compileComponents();
  });

  it('loads and displays categories', () => {
    const fixture = TestBed.createComponent(CategoryPage);
    fixture.detectChanges();

    expect(categoryApi.list).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('Groceries');
  });

  it('creates a category and reloads the list', () => {
    categoryApi.list
      .mockReturnValueOnce(of([]))
      .mockReturnValueOnce(of([groceries]));

    const fixture = TestBed.createComponent(CategoryPage);
    fixture.detectChanges();

    const page = fixture.componentInstance;
    page.form.setValue({ name: 'Groceries', color: '#4caf50', emoji: '' });
    page.save();

    expect(categoryApi.create).toHaveBeenCalledWith({
      name: 'Groceries',
      color: '#4caf50',
      emoji: null,
    });
    expect(categoryApi.list).toHaveBeenCalledTimes(2);
    expect(page.categories).toEqual([groceries]);
  });

  it('does not create when the form is invalid', () => {
    const fixture = TestBed.createComponent(CategoryPage);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({ name: '', color: '#4caf50', emoji: '' });
    fixture.componentInstance.save();

    expect(categoryApi.create).not.toHaveBeenCalled();
  });
});