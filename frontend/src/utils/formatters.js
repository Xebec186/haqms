import dayjs from 'dayjs';

export const fmtDate = (d) =>
  d ? dayjs(d).format('DD MMM YYYY') : '—';

export const fmtTime = (t) =>
  t ? String(t).slice(0, 5) : '—';

export const fmtDateTime = (dt) =>
  dt ? dayjs(dt).format('DD MMM YYYY, HH:mm') : '—';

export const fmtName = (first, last) =>
  [first, last].filter(Boolean).join(' ') || '—';

export const fmtPhone = (phone) =>
  phone || '—';
