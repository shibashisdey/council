import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authState: AuthStateService, private router: Router) {}

  canActivate(): boolean {
    if (this.authState.currentUser) {
      return true;
    }
    this.router.navigate(['/auth/login']);
    return false;
  }
}
