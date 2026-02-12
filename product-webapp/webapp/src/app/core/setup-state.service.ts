import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SetupStateService {
  private scheduleKey(userId: number): string {
    return `schedule-setup-complete:${userId}`;
  }

  isScheduleComplete(userId: number | null | undefined): boolean {
    if (!userId) {
      return false;
    }
    return localStorage.getItem(this.scheduleKey(userId)) === 'true';
  }

  markScheduleComplete(userId: number): void {
    localStorage.setItem(this.scheduleKey(userId), 'true');
  }

  clearScheduleComplete(userId: number): void {
    localStorage.removeItem(this.scheduleKey(userId));
  }
}
