import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface AuthUser {
  userId: number;
  email: string;
  role: 'CLIENT' | 'THERAPIST';
}

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  private readonly userSubject = new BehaviorSubject<AuthUser | null>(this.loadFromToken());
  user$ = this.userSubject.asObservable();

  get currentUser(): AuthUser | null {
    return this.userSubject.value;
  }

  setToken(token: string | null): void {
    if (!token) {
      localStorage.removeItem('token');
      this.userSubject.next(null);
      return;
    }
    localStorage.setItem('token', token);
    this.userSubject.next(this.parseToken(token));
  }

  private loadFromToken(): AuthUser | null {
    const token = localStorage.getItem('token');
    return token ? this.parseToken(token) : null;
  }

  private parseToken(token: string): AuthUser | null {
    try {
      const payload = token.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(normalized));
      return {
        userId: Number(decoded.userId),
        email: decoded.sub,
        role: decoded.role
      };
    } catch {
      return null;
    }
  }
}
