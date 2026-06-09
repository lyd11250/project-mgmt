import dayjs from 'dayjs'

export const FORMAT_DATE_TIME = 'YYYY-MM-DD HH:mm:ss'

export function formatDateTime(value?: string | number | null): string {
  if (!value) return '-'
  return dayjs(value).format(FORMAT_DATE_TIME)
}
