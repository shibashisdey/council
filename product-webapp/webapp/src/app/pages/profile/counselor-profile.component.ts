import { Component, OnInit } from '@angular/core';
import { AvailabilityService, CounselorSchedule, UpcomingLeave, WorkingHours } from '../../services/availability.service';
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
  leaves: UpcomingLeave[] = [];
  leavesLoading = false;
  activeSection: 'profile' | 'schedule' | 'leave' = 'profile';
  selectedDay: string | null = null;
  personalEditMode = false;

  form = {
    fullName: '',
    qualification: '',
    experienceYears: 0,
    bio: '',
    pricePerSession: 0,
    specializations: '',
    active: true
  };
  specializationTags: string[] = [];
  specializationInput = '';

  days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  hourOptions = Array.from({ length: 24 }, (_, i) => i);
  scheduleDays = this.days.map(day => ({
    day,
    enabled: false,
    persisted: false,
    startHour: 9,
    endHour: 17,
    lunchEnabled: false,
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
        this.specializationTags = profile.specializations ? [...profile.specializations] : [];
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
    const lunchEnabled = !!schedule.lunchBreak;
    const lunchStart = schedule.lunchBreak ? this.toHour(schedule.lunchBreak.startTime) : 13;

    this.scheduleDays = this.days.map(day => {
      const existing = workingHours.find(entry => entry.dayOfWeek === day);
      if (existing) {
        return {
          day,
          enabled: true,
          persisted: true,
          startHour: this.toHour(existing.startTime),
          endHour: this.toHour(existing.endTime),
          lunchEnabled,
          lunchStartHour: lunchStart
        };
      }
      return {
        day,
        enabled: false,
        persisted: false,
        startHour: 9,
        endHour: 17,
        lunchEnabled: false,
        lunchStartHour: lunchStart
      };
    });

    if (workingHours.length > 1 && !this.allWorkingHoursMatch(workingHours)) {
      this.scheduleNotice = 'Multiple working hour ranges found. Adjust each day then save.';
    } else {
      this.scheduleNotice = '';
    }
    this.selectedDay = null;
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
    const target = this.scheduleDays.find(entry => entry.day === day);
    if (!target) {
      return;
    }
    if (this.selectedDay === day) {
      this.selectedDay = null;
      return;
    }
    this.selectedDay = day;
    this.clampDayValues(target);
    this.scheduleError = '';
    this.scheduleMessage = '';
  }

  private isValidWorkingHours(day: { startHour: number; endHour: number }): boolean {
    const start = day.startHour;
    const end = day.endHour;
    return end > start && end - start <= 8;
  }

  addHour(value: number): string {
    const total = value + 1;
    const h = total % 24;
    return `${String(h).padStart(2, '0')}:00`;
  }

  formatHour(value: number): string {
    const hour = value % 24;
    return `${String(hour).padStart(2, '0')}:00`;
  }

  formatHourLabel(value: number): string {
    const hour = value % 24;
    const suffix = hour >= 12 ? 'PM' : 'AM';
    const h12 = hour % 12 === 0 ? 12 : hour % 12;
    return `${String(h12).padStart(2, '0')}:00 ${suffix}`;
  }

  get selectedDayConfig() {
    if (!this.selectedDay) {
      return null;
    }
    return this.scheduleDays.find(day => day.day === this.selectedDay) || null;
  }

  enabledDaysCount(): number {
    return this.scheduleDays.filter(day => day.enabled).length;
  }

  dayRangeText(day: { enabled: boolean; startHour: number; endHour: number }): string {
    if (!day.enabled) {
      return 'Not selected';
    }
    return `${this.formatHour(day.startHour)} - ${this.formatHour(day.endHour)}`;
  }

  onTimeChanged(): void {
    const selected = this.selectedDayConfig;
    if (!selected) {
      return;
    }
    this.clampDayValues(selected);
  }

  private clampDayValues(day: { startHour: number; endHour: number; lunchEnabled: boolean; lunchStartHour: number }): void {
    if (day.endHour <= day.startHour) {
      day.endHour = Math.min(23, day.startHour + 1);
    }
    if (day.endHour - day.startHour > 8) {
      day.endHour = Math.min(23, day.startHour + 8);
    }
    if (!day.lunchEnabled) {
      return;
    }
    if (day.lunchStartHour < day.startHour) {
      day.lunchStartHour = day.startHour;
    }
    if (day.lunchStartHour + 1 > day.endHour) {
      day.lunchStartHour = Math.max(day.startHour, day.endHour - 1);
    }
  }

  toggleLunchBreak(enabled: boolean): void {
    const selected = this.selectedDayConfig;
    if (!selected) {
      return;
    }
    selected.lunchEnabled = enabled;
    this.clampDayValues(selected);
  }

  totalHours(day: { startHour: number; endHour: number }): number {
    return Math.max(0, day.endHour - day.startHour);
  }

  effectiveHours(day: { startHour: number; endHour: number; lunchEnabled: boolean }): number {
    return Math.max(0, day.endHour - day.startHour - (day.lunchEnabled ? 1 : 0));
  }

  saveSchedule(): void {
    if (!this.profile) {
      this.scheduleError = 'Counselor profile is required.';
      return;
    }
    const selected = this.selectedDayConfig;
    if (!selected) {
      this.scheduleError = 'Select a day card to edit working hours.';
      return;
    }
    if (!this.isValidWorkingHours(selected)) {
      this.scheduleError = 'Working span must be 8 hours or less and end after start time.';
      return;
    }
    if (!selected.enabled && this.enabledDaysCount() >= 6) {
      this.scheduleError = 'You can select up to 6 working days per week.';
      return;
    }
    if (!this.isValidLunchBreak(selected)) {
      this.scheduleError = 'Lunch break must be within working hours and fixed to 1 hour.';
      return;
    }
    this.scheduleMessage = '';
    this.scheduleError = '';
    this.scheduleLoading = true;
    const counselorId = this.profile.id;
    const lunchStartHour = selected.lunchEnabled ? selected.lunchStartHour : null;
    const lunchEndHour = selected.lunchEnabled && lunchStartHour !== null ? (lunchStartHour + 1) % 24 : null;

    this.availabilityService.setWorkingHoursSafe(counselorId, {
      dayOfWeek: selected.day,
      startTime: this.formatHour(selected.startHour),
      endTime: this.formatHour(selected.endHour),
      lunchStartTime: lunchStartHour === null ? null : this.formatHour(lunchStartHour),
      lunchEndTime: lunchEndHour === null ? null : this.formatHour(lunchEndHour)
    }).subscribe({
      next: response => {
        this.scheduleLoading = false;
        if (response.status === 'SCHEDULED_FOR') {
          const dateLabel = this.formatIsoDate(response.effectiveFromDate);
          this.scheduleMessage = `${selected.day} change scheduled for ${dateLabel} (${response.conflictCount} conflicts).`;
        } else {
          this.scheduleMessage = response.message || `${selected.day} schedule updated immediately.`;
        }
        selected.enabled = true;
        selected.persisted = true;
        this.selectedDay = null;
        this.scheduleNotice = '';
      },
      error: err => {
        this.scheduleLoading = false;
        this.scheduleError = err?.error?.message || 'Unable to update schedule.';
      }
    });
  }

  removeSelectedDay(): void {
    if (!this.profile) {
      this.scheduleError = 'Counselor profile is required.';
      return;
    }
    const selected = this.selectedDayConfig;
    if (!selected || !selected.enabled) {
      this.scheduleError = 'Select an active working day to remove.';
      return;
    }

    this.scheduleMessage = '';
    this.scheduleError = '';
    this.scheduleLoading = true;
    this.availabilityService.removeWorkingDaySafe(this.profile.id, selected.day).subscribe({
      next: response => {
        this.scheduleLoading = false;
        if (response.status === 'SCHEDULED_FOR') {
          const dateLabel = this.formatIsoDate(response.effectiveFromDate);
          this.scheduleMessage = `${selected.day} removal scheduled for ${dateLabel} (${response.conflictCount} conflicts).`;
        } else {
          this.scheduleMessage = `${selected.day} removed from working days.`;
          selected.enabled = false;
          selected.persisted = false;
          selected.startHour = 9;
          selected.endHour = 17;
          selected.lunchEnabled = false;
          selected.lunchStartHour = 13;
          this.selectedDay = null;
        }
      },
      error: err => {
        this.scheduleLoading = false;
        this.scheduleError = err?.error?.message || 'Unable to remove working day.';
      }
    });
  }

  private isValidLunchBreak(day: { startHour: number; endHour: number; lunchEnabled: boolean; lunchStartHour: number }): boolean {
    if (!day.lunchEnabled) {
      return true;
    }
    return day.lunchStartHour >= day.startHour && day.lunchStartHour + 1 <= day.endHour;
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
        this.loadUpcomingLeaves();
      },
      error: err => {
        this.scheduleLoading = false;
        this.scheduleError = err?.error?.message || 'Unable to request leave.';
      }
    });
  }

  loadUpcomingLeaves(): void {
    if (!this.profile) {
      return;
    }
    this.leavesLoading = true;
    this.availabilityService.getUpcomingLeaves(this.profile.id).subscribe({
      next: leaves => {
        this.leaves = leaves || [];
        this.leavesLoading = false;
      },
      error: () => {
        this.leaves = [];
        this.leavesLoading = false;
      }
    });
  }

  deleteLeave(leaveId: number): void {
    if (!this.profile) {
      return;
    }
    this.scheduleMessage = '';
    this.scheduleError = '';
    this.scheduleLoading = true;
    this.availabilityService.cancelUnavailability(this.profile.id, leaveId).subscribe({
      next: () => {
        this.scheduleLoading = false;
        this.scheduleMessage = 'Leave deleted.';
        this.loadUpcomingLeaves();
      },
      error: err => {
        this.scheduleLoading = false;
        this.scheduleError = err?.error?.message || 'Unable to delete leave.';
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
      specializations: this.specializationTags,
      active: this.form.active
    };

    this.counselorService.updateProfile(this.profile.id, payload).subscribe({
      next: profile => {
        this.profile = profile;
        this.loading = false;
        this.message = 'Profile updated.';
        this.personalEditMode = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to update counselor profile.';
      }
    });
  }

  setActive(section: 'profile' | 'schedule' | 'leave'): void {
    this.activeSection = section;
    if (section === 'leave') {
      this.loadUpcomingLeaves();
    }
  }

  setPersonalEditMode(enabled: boolean): void {
    this.personalEditMode = enabled;
  }

  get bioCount(): number {
    return this.form.bio ? this.form.bio.length : 0;
  }

  get initials(): string {
    const name = (this.form.fullName || '').trim();
    if (!name) {
      return 'C';
    }
    const parts = name.split(/\s+/).slice(0, 2);
    return parts.map(part => part[0].toUpperCase()).join('');
  }

  addSpecialization(rawValue: string): void {
    const value = rawValue.trim();
    if (!value) {
      return;
    }
    const exists = this.specializationTags.some(tag => tag.toLowerCase() === value.toLowerCase());
    if (exists) {
      this.specializationInput = '';
      return;
    }
    this.specializationTags = [...this.specializationTags, value];
    this.form.specializations = this.specializationTags.join(', ');
    this.specializationInput = '';
  }

  removeSpecialization(tag: string): void {
    this.specializationTags = this.specializationTags.filter(item => item !== tag);
    this.form.specializations = this.specializationTags.join(', ');
  }

  onSpecializationKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addSpecialization(this.specializationInput);
    }
  }

  private formatIsoDate(value?: string): string {
    if (!value) {
      return 'a future date';
    }
    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }
}
