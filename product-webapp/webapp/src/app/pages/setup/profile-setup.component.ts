import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStateService } from '../../core/auth-state.service';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';
import { UserProfile, UserService } from '../../services/user.service';

@Component({
  selector: 'app-profile-setup',
  templateUrl: './profile-setup.component.html',
  styleUrls: ['./profile-setup.component.css']
})
export class ProfileSetupComponent implements OnInit {
  userProfile: UserProfile | null = null;
  counselorProfile: CounselorProfile | null = null;
  statusMessage = '';
  loading = false;

  userProfileForm = {
    fullName: '',
    phoneNumber: '',
    gender: '',
    dateOfBirth: '',
    city: ''
  };

  counselorProfileForm = {
    fullName: '',
    qualification: '',
    experienceYears: 0,
    bio: '',
    pricePerSession: 0,
    specializations: ''
  };

  constructor(
    private authState: AuthStateService,
    private userService: UserService,
    private counselorService: CounselorService,
    private router: Router
  ) {}

  get isTherapist(): boolean {
    return this.authState.currentUser?.role === 'THERAPIST';
  }

  ngOnInit(): void {
    if (this.isTherapist) {
      this.loadCounselorProfile();
    } else {
      this.loadUserProfile();
    }
  }

  private loadUserProfile(): void {
    this.userService.getMe().subscribe({
      next: profile => {
        this.userProfile = profile;
        const hasAge = profile?.age !== null && profile?.age !== undefined;
        const isComplete = !!(profile && profile.fullName && profile.gender && hasAge);
        if (isComplete) {
          this.router.navigate(['/dashboard']);
          return;
        }
        this.userProfileForm = {
          fullName: profile.fullName || '',
          phoneNumber: profile.phoneNumber || '',
          gender: profile.gender || '',
          dateOfBirth: this.normalizeDateInput((profile as any).dateOfBirth),
          city: profile.city || ''
        };
      },
      error: () => {
        this.userProfile = null;
      }
    });
  }

  private loadCounselorProfile(): void {
    this.counselorService.getMe().subscribe({
      next: profile => {
        this.counselorProfile = profile;
        const isComplete = !!(profile && profile.fullName && profile.qualification);
        if (isComplete) {
          this.router.navigate(['/setup/schedule']);
          return;
        }
        this.counselorProfileForm = {
          fullName: profile.fullName || '',
          qualification: profile.qualification || '',
          experienceYears: profile.experienceYears || 0,
          bio: profile.bio || '',
          pricePerSession: profile.pricePerSession || 0,
          specializations: profile.specializations?.join(', ') || ''
        };
      },
      error: () => {
        this.counselorProfile = null;
      }
    });
  }

  saveUserProfile(): void {
    this.loading = true;
    const payload = {
      ...this.userProfileForm,
      dateOfBirth: this.userProfileForm.dateOfBirth || null
    };

    const request = this.userProfile
      ? this.userService.updateMe(payload)
      : this.userService.createProfile(payload);

    request.subscribe({
      next: profile => {
        this.userProfile = profile;
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading = false;
        this.handleError(err);
      }
    });
  }

  saveCounselorProfile(): void {
    this.loading = true;
    const payload = {
      ...this.counselorProfileForm,
      specializations: this.counselorProfileForm.specializations
        .split(',')
        .map(s => s.trim())
        .filter(Boolean)
    };

    const request = this.counselorProfile
      ? this.counselorService.updateProfile(this.counselorProfile.id, payload)
      : this.counselorService.createProfile(payload);

    request.subscribe({
      next: profile => {
        this.counselorProfile = profile;
        this.loading = false;
        this.router.navigate(['/setup/schedule']);
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
}
