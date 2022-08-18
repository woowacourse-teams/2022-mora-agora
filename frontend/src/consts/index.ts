import { QueryStatus } from 'types/queryType';
import { AttendanceStatus } from 'types/userType';

export const TOKEN_ERROR_STATUS_CODES = [401, 404];

export const NOT_FOUND_STATUS_CODE = 404;

export const QUERY_STATUS: Record<string, QueryStatus> = {
  LOADING: 'loading',
  ERROR: 'error',
  SUCCESS: 'success',
};

export const MUTATION_STATUS: Record<string, QueryStatus> = {
  IDLE: 'idle',
  LOADING: 'loading',
  ERROR: 'error',
  SUCCESS: 'success',
};

export const ATTENDANCE_STATUS: Record<AttendanceStatus, boolean> = {
  tardy: false,
  present: true,
  none: false,
} as const;
