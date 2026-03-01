import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserProfile, UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit {
  profile: UserProfile | null = null;
  loading = false;
  message = '';
  error = '';
  today = new Date().toISOString().split('T')[0];
  editMode = false;

  form = {
    fullName: '',
    phoneNumber: '',
    countryCode: '+91',
    gender: '',
    dateOfBirth: '',
    city: ''
  };

  constructor(private userService: UserService, private router: Router) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.userService.getMe().subscribe({
      next: profile => {
        this.profile = profile;
        this.form.fullName = profile.fullName || '';
        this.form.gender = profile.gender || '';
        this.form.dateOfBirth = this.normalizeDateInput((profile as any).dateOfBirth);
        this.form.countryCode = '+91';
        this.form.phoneNumber = '';
        if (profile.phoneNumber) {
          const match = profile.phoneNumber.match(/^(\+\d{1,4})\s*(.*)$/);
          if (match) {
            this.form.countryCode = match[1];
            this.form.phoneNumber = match[2];
          } else {
            this.form.phoneNumber = profile.phoneNumber;
          }
        }
        this.form.city = profile.city || '';
        this.editMode = !this.hasEssentialProfile();
      },
      error: () => {
        this.error = 'Unable to load profile.';
      }
    });
  }

  save(): void {
    this.message = '';
    this.error = '';
    this.loading = true;
    const rawPhone = this.form.phoneNumber.trim();
    const payload = {
      ...this.form,
      phoneNumber: rawPhone ? `${this.form.countryCode} ${rawPhone}`.trim() : '',
      dateOfBirth: this.form.dateOfBirth || null
    };
    this.userService.updateMe(payload).subscribe({
      next: profile => {
        this.profile = profile;
        this.loading = false;
        this.message = 'Profile updated.';
        this.editMode = false;
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to update profile.';
      }
    });
  }

  private normalizeDateInput(value?: string | null): string {
    if (!value) {
      return '';
    }
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return value;
    }
    if (/^\d{2}-\d{2}-\d{4}$/.test(value)) {
      const [day, month, year] = value.split('-');
      return `${year}-${month}-${day}`;
    }
    const parsed = new Date(value);
    if (!isNaN(parsed.getTime())) {
      return parsed.toISOString().split('T')[0];
    }
    return '';
  }

  startEdit(): void {
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
    this.message = '';
    this.error = '';
    if (this.profile) {
      this.form.fullName = this.profile.fullName || '';
      this.form.gender = this.profile.gender || '';
      this.form.dateOfBirth = this.normalizeDateInput((this.profile as any).dateOfBirth);
      this.form.countryCode = '+91';
      this.form.phoneNumber = '';
      if (this.profile.phoneNumber) {
        const match = this.profile.phoneNumber.match(/^(\+\d{1,4})\s*(.*)$/);
        if (match) {
          this.form.countryCode = match[1];
          this.form.phoneNumber = match[2];
        } else {
          this.form.phoneNumber = this.profile.phoneNumber;
        }
      }
      this.form.city = this.profile.city || '';
    }
  }

  displayValue(value?: string | null): string {
    return value && value.trim() ? value : 'Not provided';
  }

  displayPhone(): string {
    if (!this.profile?.phoneNumber) {
      return 'Not provided';
    }
    return this.profile.phoneNumber;
  }

  displayDob(): string {
    const raw = (this.profile as any)?.dateOfBirth;
    const normalized = this.normalizeDateInput(raw);
    return normalized || 'Not provided';
  }

  private hasEssentialProfile(): boolean {
    return !!(
      this.profile &&
      this.profile.fullName &&
      this.profile.gender &&
      (this.profile as any).dateOfBirth
    );
  }
}
