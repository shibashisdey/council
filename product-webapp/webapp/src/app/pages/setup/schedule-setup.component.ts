import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthStateService } from '../../core/auth-state.service';
import { SetupStateService } from '../../core/setup-state.service';
import { AvailabilityService } from '../../services/availability.service';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';

@Component({
  selector: 'app-schedule-setup',
  templateUrl: './schedule-setup.component.html',
  styleUrls: ['./schedule-setup.component.css']
})
export class ScheduleSetupComponent implements OnInit {
  counselorProfile: CounselorProfile | null = null;
  statusMessage = '';
  loading = false;

  workingHoursForm = {
    dayOfWeek: 'MONDAY',
    startTime: '09:00',
    endTime: '17:00'
  };

  lunchBreakForm = {
    startTime: '13:00',
    endTime: '14:00'
  };

  constructor(
    private authState: AuthStateService,
    private counselorService: CounselorService,
    private availabilityService: AvailabilityService,
    private setupState: SetupStateService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.counselorService.getMe().subscribe({
      next: profile => {
        this.counselorProfile = profile;
      },
      error: () => {
        this.statusMessage = 'Unable to load counselor profile.';
      }
    });
  }

  saveSchedule(): void {
    if (!this.counselorProfile) {
      this.statusMessage = 'Counselor profile is required.';
      return;
    }
    this.loading = true;
    const counselorId = this.counselorProfile.id;
    forkJoin([
      this.availabilityService.setWorkingHours(counselorId, this.workingHoursForm),
      this.availabilityService.setLunchBreak(counselorId, this.lunchBreakForm)
    ]).subscribe({
      next: () => {
        const userId = this.authState.currentUser?.userId;
        if (userId) {
          this.setupState.markScheduleComplete(userId);
        }
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading = false;
        this.handleError(err);
      }
    });
  }

  private handleError(err: any): void {
    const message = err?.error?.message || err?.message || 'Request failed';
    this.statusMessage = message;
  }
}
