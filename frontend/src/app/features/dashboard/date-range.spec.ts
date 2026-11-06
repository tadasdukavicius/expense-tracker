import { describe, expect, it } from 'vitest';
import { presetRange } from './date-range';

describe('presetRange', () => {
  const today = new Date(2026, 9, 17);

  it('covers the whole current month', () => {
    expect(presetRange('thisMonth', today)).toEqual({ from: '2026-10-01', to: '2026-10-31' });
  });

  it('covers the previous month, including across a year boundary', () => {
    expect(presetRange('lastMonth', today)).toEqual({ from: '2026-09-01', to: '2026-09-30' });
    expect(presetRange('lastMonth', new Date(2026, 0, 5))).toEqual({ from: '2025-12-01', to: '2025-12-31' });
  });

  it('covers the last 30 days inclusive', () => {
    expect(presetRange('last30', today)).toEqual({ from: '2026-09-18', to: '2026-10-17' });
  });

  it('is open-ended for all time', () => {
    expect(presetRange('all')).toEqual({ from: null, to: null });
  });
});
