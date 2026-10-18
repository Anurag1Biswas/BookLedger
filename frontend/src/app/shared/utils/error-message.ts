import { HttpErrorResponse } from '@angular/common/http';

/**
 * Turns whatever the backend sent into one friendly sentence.
 * Handles the new JSON error shape ({message, fieldErrors}) and the older plain-text bodies.
 */
export function errorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (!(err instanceof HttpErrorResponse)) return fallback;
  if (err.status === 0) return 'Cannot reach the server. Check your connection and try again.';

  const body = err.error;
  if (typeof body === 'string' && body.trim()) {
    return body.length > 200 ? fallback : body;
  }
  if (body && typeof body === 'object') {
    const fields = body.fieldErrors as Record<string, string> | undefined;
    if (fields && Object.keys(fields).length) {
      return Object.entries(fields).map(([f, m]) => `${f}: ${m}`).join('; ');
    }
    if (typeof body.message === 'string' && body.message) return body.message;
  }
  switch (err.status) {
    case 401: return 'Your session has expired. Please sign in again.';
    case 403: return 'You do not have permission to do that.';
    case 404: return 'That item could not be found.';
    case 409: return 'This change conflicts with existing data.';
    default: return err.status >= 500 ? 'The server had a problem. Please try again shortly.' : fallback;
  }
}
