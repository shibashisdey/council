import { Component, OnInit } from '@angular/core';
import { AuthStateService, AuthUser } from '../../core/auth-state.service';
import { UserService, UserProfile } from '../../services/user.service';
import { CounselorService, CounselorProfile } from '../../services/counselor.service';
import { AvailabilityService, AvailabilityBlock } from '../../services/availability.service';
import { AppointmentService, Appointment, CounselorAppointment } from '../../services/appointment.service';
import { PaymentService, PaymentResponse } from '../../services/payment.service';
import { ReviewService, Review } from '../../services/review.service';
import { SessionNoteService, SessionNote } from '../../services/session-note.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: AuthUser | null = null;

  userProfile: UserProfile | null = null;
  counselorProfile: CounselorProfile | null = null;

  counselors: CounselorProfile[] = [];
  selectedCounselor: CounselorProfile | null = null;
  availabilityDate = '';
  availabilityBlocks: AvailabilityBlock[] = [];

  appointments: Appointment[] = [];
  counselorAppointments: CounselorAppointment[] = [];

  reviews: Review[] = [];
  sessionNotes: SessionNote[] = [];

  statusMessage = '';

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

  bookingForm = {
    counselorId: 0,
    appointmentDate: '',
    startTime: ''
  };

  rescheduleForm = {
    appointmentId: 0,
    newDate: '',
    newStartTime: ''
  };

  paymentForm = {
    appointmentId: 0,
    amount: 0
  };

  reviewForm = {
    appointmentId: 0,
    rating: 5,
    comment: ''
  };

  noteForm = {
    appointmentId: 0,
    sessionDate: '',
    summary: '',
    observations: '',
    recommendations: '',
    privateNotes: ''
  };

  workingHoursForm = {
    dayOfWeek: 'MONDAY',
    startTime: '09:00',
    endTime: '17:00'
  };

  lunchBreakForm = {
    startTime: '13:00',
    endTime: '14:00'
  };

  unavailabilityForm = {
    date: '',
    startTime: '',
    endTime: '',
    reason: 'COUNSELOR_LEAVE'
  };

  constructor(
    private authState: AuthStateService,
    private userService: UserService,
    private counselorService: CounselorService,
    private availabilityService: AvailabilityService,
    private appointmentService: AppointmentService,
    private paymentService: PaymentService,
    private reviewService: ReviewService,
    private sessionNoteService: SessionNoteService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authState.currentUser;
    this.refreshProfiles();
    this.loadCounselors();
    this.loadAppointments();
    this.loadReviews();
    this.loadSessionNotes();
  }

  refreshProfiles(): void {
    this.userService.getMe().subscribe({
      next: profile => {
        this.userProfile = profile;
        this.userProfileForm = {
          fullName: profile.fullName || '',
          phoneNumber: profile.phoneNumber || '',
          gender: profile.gender || '',
          dateOfBirth: '',
          city: profile.city || ''
        };
      },
      error: () => {
        this.userProfile = null;
      }
    });

    if (this.currentUser?.role === 'THERAPIST') {
      this.counselorService.getMe().subscribe({
        next: profile => {
          this.counselorProfile = profile;
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
  }

  createUserProfile(): void {
    const payload = {
      ...this.userProfileForm,
      dateOfBirth: this.userProfileForm.dateOfBirth || null
    };
    this.userService.createProfile(payload).subscribe({
      next: profile => {
        this.userProfile = profile;
        this.statusMessage = 'User profile created.';
      },
      error: err => this.handleError(err)
    });
  }

  updateUserProfile(): void {
    const payload = {
      ...this.userProfileForm,
      dateOfBirth: this.userProfileForm.dateOfBirth || null
    };
    this.userService.updateMe(payload).subscribe({
      next: profile => {
        this.userProfile = profile;
        this.statusMessage = 'User profile updated.';
      },
      error: err => this.handleError(err)
    });
  }

  createCounselorProfile(): void {
    const payload = {
      ...this.counselorProfileForm,
      specializations: this.counselorProfileForm.specializations
        .split(',')
        .map(s => s.trim())
        .filter(Boolean)
    };
    this.counselorService.createProfile(payload).subscribe({
      next: profile => {
        this.counselorProfile = profile;
        this.statusMessage = 'Counselor profile created.';
      },
      error: err => this.handleError(err)
    });
  }

  updateCounselorProfile(): void {
    if (!this.counselorProfile) {
      return;
    }
    const payload = {
      ...this.counselorProfileForm,
      specializations: this.counselorProfileForm.specializations
        .split(',')
        .map(s => s.trim())
        .filter(Boolean)
    };
    this.counselorService.updateProfile(this.counselorProfile.id, payload).subscribe({
      next: profile => {
        this.counselorProfile = profile;
        this.statusMessage = 'Counselor profile updated.';
      },
      error: err => this.handleError(err)
    });
  }

  loadCounselors(): void {
    this.counselorService.listActive().subscribe({
      next: data => this.counselors = data,
      error: err => this.handleError(err)
    });
  }

  selectCounselor(counselorId: number): void {
    this.counselorService.getById(counselorId).subscribe({
      next: counselor => {
        this.selectedCounselor = counselor;
        this.bookingForm.counselorId = counselor.id;
      },
      error: err => this.handleError(err)
    });
  }

  loadAvailability(): void {
    if (!this.selectedCounselor || !this.availabilityDate) {
      return;
    }
    this.availabilityService.getCalendar(this.selectedCounselor.id, this.availabilityDate).subscribe({
      next: blocks => this.availabilityBlocks = blocks,
      error: err => this.handleError(err)
    });
  }

  bookAppointment(): void {
    this.appointmentService.create(this.bookingForm).subscribe({
      next: appointment => {
        this.statusMessage = `Appointment created (${appointment.status}).`;
        this.loadAppointments();
      },
      error: err => this.handleError(err)
    });
  }

  loadAppointments(): void {
    if (!this.currentUser) {
      return;
    }
    if (this.currentUser.role === 'CLIENT') {
      this.appointmentService.listForClient().subscribe({
        next: data => this.appointments = data,
        error: err => this.handleError(err)
      });
    } else {
      this.counselorService.getMe().subscribe({
        next: counselor => {
          this.counselorProfile = counselor;
          this.appointmentService.listForCounselor(counselor.id).subscribe({
            next: data => this.counselorAppointments = data,
            error: err => this.handleError(err)
          });
        },
        error: err => this.handleError(err)
      });
    }
  }

  rescheduleAppointment(): void {
    this.appointmentService.reschedule(this.rescheduleForm.appointmentId, {
      newDate: this.rescheduleForm.newDate,
      newStartTime: this.rescheduleForm.newStartTime
    }).subscribe({
      next: () => {
        this.statusMessage = 'Appointment rescheduled.';
        this.loadAppointments();
      },
      error: err => this.handleError(err)
    });
  }

  cancelAppointment(appointmentId: number): void {
    this.appointmentService.cancel(appointmentId).subscribe({
      next: () => {
        this.statusMessage = 'Appointment cancelled.';
        this.loadAppointments();
      },
      error: err => this.handleError(err)
    });
  }

  createPayment(): void {
    this.paymentService.createPayment(this.paymentForm).subscribe({
      next: (payment: PaymentResponse) => {
        this.statusMessage = `Payment created (${payment.status}).`;
      },
      error: err => this.handleError(err)
    });
  }

  simulatePaymentSuccess(appointmentId: number): void {
    this.paymentService.simulateSuccess(appointmentId).subscribe({
      next: () => {
        this.statusMessage = 'Payment confirmed (dev).';
        this.loadAppointments();
      },
      error: err => this.handleError(err)
    });
  }

  loadReviews(): void {
    if (!this.currentUser) {
      return;
    }
    if (this.currentUser.role === 'CLIENT') {
      this.reviewService.getReviewsForUser(this.currentUser.userId).subscribe({
        next: data => this.reviews = data,
        error: err => this.handleError(err)
      });
    } else {
      this.counselorService.getMe().subscribe({
        next: counselor => {
          this.counselorProfile = counselor;
          this.reviewService.getReviewsForCounselor(counselor.id).subscribe({
            next: data => this.reviews = data,
            error: err => this.handleError(err)
          });
        },
        error: err => this.handleError(err)
      });
    }
  }

  createReview(): void {
    this.reviewService.createReview(this.reviewForm).subscribe({
      next: () => {
        this.statusMessage = 'Review created.';
        this.loadReviews();
      },
      error: err => this.handleError(err)
    });
  }

  loadSessionNotes(): void {
    if (!this.currentUser) {
      return;
    }
    if (this.currentUser.role === 'CLIENT') {
      this.sessionNoteService.getNotesForUser(this.currentUser.userId).subscribe({
        next: data => this.sessionNotes = data,
        error: err => this.handleError(err)
      });
    } else {
      this.counselorService.getMe().subscribe({
        next: counselor => {
          this.counselorProfile = counselor;
          this.sessionNoteService.getNotesForCounselor(counselor.id).subscribe({
            next: data => this.sessionNotes = data,
            error: err => this.handleError(err)
          });
        },
        error: err => this.handleError(err)
      });
    }
  }

  createSessionNote(): void {
    const payload = {
      ...this.noteForm,
      sessionDate: this.noteForm.sessionDate || null
    };
    this.sessionNoteService.create(payload).subscribe({
      next: () => {
        this.statusMessage = 'Session note created.';
        this.loadSessionNotes();
      },
      error: err => this.handleError(err)
    });
  }

  shareSessionNote(noteId: number, shared: boolean): void {
    this.sessionNoteService.share(noteId, { shared }).subscribe({
      next: () => {
        this.statusMessage = 'Session note updated.';
        this.loadSessionNotes();
      },
      error: err => this.handleError(err)
    });
  }

  setWorkingHours(): void {
    if (!this.counselorProfile) {
      return;
    }
    this.availabilityService.setWorkingHours(this.counselorProfile.id, this.workingHoursForm).subscribe({
      next: () => this.statusMessage = 'Working hours set.',
      error: err => this.handleError(err)
    });
  }

  setLunchBreak(): void {
    if (!this.counselorProfile) {
      return;
    }
    this.availabilityService.setLunchBreak(this.counselorProfile.id, this.lunchBreakForm).subscribe({
      next: () => this.statusMessage = 'Lunch break set.',
      error: err => this.handleError(err)
    });
  }

  addUnavailability(): void {
    if (!this.counselorProfile) {
      return;
    }
    this.availabilityService.addUnavailability(this.counselorProfile.id, this.unavailabilityForm).subscribe({
      next: () => this.statusMessage = 'Unavailability added.',
      error: err => this.handleError(err)
    });
  }

  private handleError(err: any): void {
    const message = err?.error?.message || err?.message || 'Request failed';
    this.statusMessage = message;
  }
}
