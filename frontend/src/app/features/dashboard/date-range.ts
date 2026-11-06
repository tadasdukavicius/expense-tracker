export type RangePreset = 'thisMonth' | 'lastMonth' | 'last30' | 'thisYear' | 'all' | 'custom';

export interface DateRange {
  from: string | null;
  to: string | null;
}

const iso = (d: Date): string =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

export function presetRange(preset: Exclude<RangePreset, 'custom'>, today = new Date()): DateRange {
  const y = today.getFullYear();
  const m = today.getMonth();
  switch (preset) {
    case 'thisMonth':
      return { from: iso(new Date(y, m, 1)), to: iso(new Date(y, m + 1, 0)) };
    case 'lastMonth':
      return { from: iso(new Date(y, m - 1, 1)), to: iso(new Date(y, m, 0)) };
    case 'last30':
      return { from: iso(new Date(y, today.getMonth(), today.getDate() - 29)), to: iso(today) };
    case 'thisYear':
      return { from: iso(new Date(y, 0, 1)), to: iso(new Date(y, 11, 31)) };
    case 'all':
      return { from: null, to: null };
  }
}
