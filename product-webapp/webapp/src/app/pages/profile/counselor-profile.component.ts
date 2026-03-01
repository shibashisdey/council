import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AvailabilityService, CounselorSchedule, WorkingHours } from '../../services/availability.service';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';

@Component({
  selector: 'app-counselor-profile',
  templateUrl: './counselor-profile.component.html',
  styleUrls: ['./counselor-profile.component.css']
})
export class CounselorProfileComponent implements OnInit {
  profile: CounselorProfile | null = null;
  loading = false;
  message = '';
  error = '';
  scheduleMessage = '';
  scheduleError = '';
  scheduleLoading = false;
  scheduleNotice = '';
  activeSection: 'profile' | 'schedule' | 'leave' = 'profile';

  form = {
    fullName: '',
    qualification: '',
    experienceYears: 0,
    bio: '',
    pricePerSession: 0,
    specializations: '',
    active: true
  };

  days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  hourOptions = Array.from({ length: 24 }, (_, i) => i);
  endHourOptions = Array.from({ length: 24 }, (_, i) => i + 1);
  scheduleDays = this.days.map(day => ({
    day,
    enabled: false,
    startHour: 9,
    endHour: 17,
    lunchStartHour: 13
  }));

  leaveForm = {
    date: '',
    startTime: '09:00',
    endTime: '10:00',
    reason: 'COUNSELOR_LEAVE'
  };

  constructor(
    private counselorService: CounselorService,
    private availabilityService: AvailabilityService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.counselorService.getMe().subscribe({
      next: profile => {
        this.profile = profile;
        this.form.fullName = profile.fullName || '';
        this.form.qualification = profile.qualification || '';
        this.form.experienceYears = profile.experienceYears || 0;
        this.form.bio = profile.bio || '';
        this.form.pricePerSession = profile.pricePerSession || 0;
        this.form.specializations = profile.specializations?.join(', ') || '';
        this.form.active = profile.active ?? true;
        this.loadSchedule();
      },
      error: () => {
        this.error = 'Unable to load counselor profile.';
      }
    });
  }

  private loadSchedule(): void {
    if (!this.profile) {
      return;
    }
    this.availabilityService.getSchedule(this.profile.id).subscribe({
      next: schedule => this.applySchedule(schedule),
      error: () => {
        this.scheduleNotice = 'No saved schedule yet. Set your weekly hours below.';
      }
    });
  }

  private applySchedule(schedule: CounselorSchedule): void {
    const workingHours = schedule.workingHours || [];
    const lunchStart = schedule.lunchBreak
      ? this.toHour(schedule.lunchBreak.startTime)
      : 13;

    this.scheduleDays = this.days.map(day => {
      const existing = workingHours.find(entry => entry.dayOfWeek === day);
      if (existing) {
        return {
          day,
          enabled: true,
          startHour: this.toHour(existing.startTime),
          endHour: this.toHour(existing.endTime),
          lunchStartHour: lunchStart
        };
      }
      return {
        day,
        enabled: false,
        startHour: 9,
        endHour: 17,
        lunchStartHour: lunchStart
      };
    });

    if (workingHours.length > 1 && !this.allWorkingHoursMatch(workingHours)) {
      this.scheduleNotice = 'Multiple working hour ranges found. Adjust each day then save.';
    } else {
      this.scheduleNotice = '';
    }
  }

  private allWorkingHoursMatch(hours: WorkingHours[]): boolean {
    if (hours.length === 0) {
      return true;
    }
    const first = hours[0];
    return hours.every(entry =>
      this.toTimeString(entry.startTime) === this.toTimeString(first.startTime) &&
      this.toTimeString(entry.endTime) === this.toTimeString(first.endTime)
    );
  }

  private toTimeString(value: string): string {
    return value ? value.substring(0, 5) : '';
  }

  private toHour(value: string): number {
    const hour = Number(this.toTimeString(value).split(':')[0]);
    return Number.isNaN(hour) ? 0 : hour;
  }

  toggleDay(day: string): void {
    const selected = this.scheduleDays.filter(entry => entry.enabled);
    const target = this.scheduleDays.find(entry => entry.day === day);
    if (!target) {
      return;
    }
    if (!target.enabled && selected.length >= 5) {
      this.scheduleError = 'You can select up to 5 working days per week.';
      return;
    }
    target.enabled = !target.enabled;
    this.scheduleError = '';
  }

  private isValidWorkingHours(day: { startHour: number; endHour: number }): boolean {
    const start = day.startHour;
    const end = day.endHour;
    return end > start && end - start <= 8;
  }

  addHour(value: number): string {
    const total = value + 1;
    if (total === 24) {
      return '24:00';
    }
    const h = total % 24;
    return `${String(h).padStart(2, '0')}:00`;
  }

  formatHour(value: number): string {
    if (value === 24) {
      return '24:00';
    }
    return `${String(value).padStart(2, '0')}:00`;
  }

  startMax(day: { endHour: number }): number {
    return Math.max(0, day.endHour - 1);
  }

  endMin(day: { startHour: number }): number {
    return Math.min(24, day.startHour + 1);
  }

  endMax(day: { startHour: number }): number {
    return Math.min(24, day.startHour + 8);
  }

  saveSchedule(): void {
    if (!this.profile) {
      this.scheduleError = 'Counselor profile is required.';
      return;
    }
    const enabledDays = this.scheduleDays.filter(entry => entry.enabled);
    if (enabledDays.length === 0) {
      this.scheduleError = 'Select at least one working day.';
      return;
    }
    if (enabledDays.some(day => !this.isValidWorkingHours(day))) {
      this.scheduleError = 'Each selected day must have max 8 working hours and end after start.';
      return;
    }
    if (!this.isValidLunchBreak(enabledDays)) {
      this.scheduleError = 'Lunch break must be within working hours and fixed to 1 hour.';
      return;
    }
    this.scheduleMessage = '';
    this.scheduleError = '';
    this.scheduleLoading = true;
    const counselorId = this.profile.id;
    const lunchStartHour = enabledDays[0].lunchStartHour;
    const lunchEnd = this.addHour(lunchStartHour);
    const workingRequests = enabledDays.map(day =>
      this.availabilityService.setWorkingHours(counselorId, {
        dayOfWeek: day.day,
        startTime: this.formatHour(day.startHour),
        endTime: this.formatHour(day.endHour)
      })
    );
    const requests = [
      ...workingRequests,
      this.availabilityService.setLunchBreak(counselorId, {
        startTime: this.formatHour(lunchStartHour),
        endTime: lunchEnd
      })
    ];

    forkJoin(requests).subscribe({
      next: () => {
        this.scheduleLoading = false;
        this.scheduleMessage = 'Schedule updated.';
        this.scheduleNotice = '';
      },
      error: err => {
        this.scheduleLoading = false;
        this.scheduleError = err?.error?.message || 'Unable to update schedule.';
      }
    });
  }

  private isValidLunchBreak(days: Array<{ startHour: number; endHour: number; lunchStartHour: number }>): boolean {
    const reference = days[0].lunchStartHour;
    if (!days.every(day => day.lunchStartHour === reference)) {
      this.scheduleNotice = 'Lunch break time must be consistent across selected days.';
      return false;
    }
    return days.every(day => reference >= day.startHour && reference + 1 <= day.endHour);
  }

  requestLeave(): void {
    if (!this.profile) {
      this.scheduleError = 'Counselor profile is required.';
      return;
    }
    if (!this.leaveForm.date) {
      this.scheduleError = 'Select a leave date.';
      return;
    }
    this.scheduleMessage = '';
    this.scheduleError = '';
    this.scheduleLoading = true;
    this.availabilityService.addUnavailability(this.profile.id, this.leaveForm).subscribe({
      next: () => {
        this.scheduleLoading = false;
        this.scheduleMessage = 'Leave request submitted.';
      },
      error: err => {
        this.scheduleLoading = false;
        this.scheduleError = err?.error?.message || 'Unable to request leave.';
      }
    });
  }

  save(): void {
    if (!this.profile) {
      return;
    }
    this.message = '';
    this.error = '';
    this.loading = true;
    const payload = {
      fullName: this.form.fullName,
      qualification: this.form.qualification,
      experienceYears: Number(this.form.experienceYears),
      bio: this.form.bio,
      pricePerSession: Number(this.form.pricePerSession),
      specializations: this.form.specializations
        .split(',')
        .map(s => s.trim())
        .filter(Boolean),
      active: this.form.active
    };

    this.counselorService.updateProfile(this.profile.id, payload).subscribe({
      next: profile => {
        this.profile = profile;
        this.loading = false;
        this.message = 'Profile updated.';
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to update counselor profile.';
      }
    });
  }

  setActive(section: 'profile' | 'schedule' | 'leave'): void {
    this.activeSection = section;
  }
}
