import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor() {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = localStorage.getItem('token');
    if (token) {
      const decoded = this.decodeToken(token);
      const headers: Record<string, string> = {
        Authorization: `Bearer ${token}`
      };
      if (decoded?.userId) {
        headers['X-USER-ID'] = String(decoded.userId);
      }
      if (decoded?.role) {
        headers['X-USER-ROLE'] = decoded.role;
      }
      if (decoded?.email) {
        headers['X-USER-EMAIL'] = decoded.email;
      }
      request = request.clone({
        setHeaders: headers
      });
    }
    return next.handle(request);
  }

  private decodeToken(token: string): { userId?: number; role?: string; email?: string } | null {
    try {
      const payload = token.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(normalized));
      return {
        userId: Number(decoded.userId),
        role: decoded.role,
        email: decoded.sub
      };
    } catch {
      return null;
    }
  }
}
