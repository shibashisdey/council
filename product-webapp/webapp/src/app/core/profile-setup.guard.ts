import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthStateService } from './auth-state.service';
import { CounselorService } from '../services/counselor.service';
import { UserService } from '../services/user.service';
import { SetupStateService } from './setup-state.service';

@Injectable({
  providedIn: 'root'
})
export class ProfileSetupGuard implements CanActivate {
  constructor(
    private authState: AuthStateService,
    private router: Router,
    private userService: UserService,
    private counselorService: CounselorService,
    private setupState: SetupStateService
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    const user = this.authState.currentUser;
    if (!user) {
      return of(this.router.createUrlTree(['/auth/login']));
    }

    if (user.role === 'CLIENT') {
      return this.userService.getMe().pipe(
        map(profile => {
          const hasAge = profile?.age !== null && profile?.age !== undefined;
          const isComplete = !!(profile
            && profile.fullName
            && profile.gender
            && hasAge);
          if (isComplete) {
            return this.router.createUrlTree(['/dashboard']);
          }
          return true;
        }),
        catchError(() => of(true))
      );
    }

    return this.counselorService.getMe().pipe(
      map(profile => {
        const isComplete = !!(profile
          && profile.fullName
          && profile.qualification);
        if (isComplete) {
          if (!this.setupState.isScheduleComplete(user.userId)) {
            return this.router.createUrlTree(['/setup/schedule']);
          }
          return this.router.createUrlTree(['/dashboard']);
        }
        return true;
      }),
      catchError(() => of(true))
    );
  }
}
