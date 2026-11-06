import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Account, CategoryStats } from '../../core/models/api.models';
import { AccountService } from '../../core/services/account.service';
import { StatsService } from '../../core/services/stats.service';
import { RangePreset, presetRange } from './date-range';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    BaseChartDirective,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage implements OnInit {
  private readonly statsApi = inject(StatsService);
  private readonly accountsApi = inject(AccountService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly presets: { value: RangePreset; label: string }[] = [
    { value: 'thisMonth', label: 'This month' },
    { value: 'lastMonth', label: 'Last month' },
    { value: 'last30', label: 'Last 30 days' },
    { value: 'thisYear', label: 'This year' },
    { value: 'all', label: 'All time' },
    { value: 'custom', label: 'Custom' },
  ];

  readonly filters = new FormGroup({
    preset: new FormControl<RangePreset>('thisMonth', { nonNullable: true }),
    from: new FormControl(''),
    to: new FormControl(''),
    accountId: new FormControl<number | null>(null),
  });

  accounts: Account[] = [];
  stats: CategoryStats | null = null;
  chartData: ChartConfiguration<'doughnut'>['data'] = { labels: [], datasets: [] };
  readonly chartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: { legend: { display: false } },
  };
  currency = 'EUR';
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.accountsApi.list().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        this.cdr.markForCheck();
      },
    });
    this.load();
  }

  onPresetChange(): void {
    const preset = this.filters.controls.preset.value;
    if (preset !== 'custom') {
      const range = presetRange(preset);
      this.filters.patchValue({ from: range.from ?? '', to: range.to ?? '' });
    }
    this.load();
  }

  load(): void {
    const value = this.filters.getRawValue();
    const range = value.preset === 'custom'
      ? { from: value.from || null, to: value.to || null }
      : presetRange(value.preset);

    this.loading = true;
    this.errorMessage = '';

    this.statsApi.byCategory(range.from ?? undefined, range.to ?? undefined, value.accountId ?? undefined)
      .subscribe({
        next: (stats) => {
          this.stats = stats;
          this.currency = this.accounts.find((a) => a.id === value.accountId)?.currency
            ?? this.accounts[0]?.currency ?? 'EUR';
          this.chartData = {
            labels: stats.categories.map((c) => c.name),
            datasets: [{
              data: stats.categories.map((c) => c.amount),
              backgroundColor: stats.categories.map((c) => c.color),
            }],
          };
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          this.errorMessage = error instanceof HttpErrorResponse && error.status === 0
            ? 'Cannot reach the backend. Check that it is running.'
            : 'Could not load statistics.';
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }
}
