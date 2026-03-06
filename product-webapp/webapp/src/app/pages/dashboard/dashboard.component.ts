import { Component, OnInit } from '@angular/core';
import { AuthStateService, AuthUser } from '../../core/auth-state.service';
import { UserProfile, UserService } from '../../services/user.service';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';
import { AppointmentService, Appointment, CounselorAppointment } from '../../services/appointment.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: AuthUser | null = null;
  userProfile: UserProfile | null = null;
  counselorProfile: CounselorProfile | null = null;

  appointments: Appointment[] = [];
  counselorAppointments: CounselorAppointment[] = [];

  statusMessage = '';

  constructor(
    private authState: AuthStateService,
    private userService: UserService,
    private counselorService: CounselorService,
    private appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authState.currentUser;
    this.loadProfile();
  }

  private loadProfile(): void {
    if (!this.currentUser) {
      return;
    }
    if (this.currentUser.role === 'CLIENT') {
      this.userService.getMe().subscribe({
        next: profile => {
          this.userProfile = profile;
          this.loadAppointments();
        },
        error: () => {
          this.userProfile = null;
        }
      });
      return;
    }

    this.counselorService.getMe().subscribe({
      next: profile => {
        this.counselorProfile = profile;
        this.loadAppointments();
      },
      error: () => {
        this.counselorProfile = null;
      }
    });
  }

  private loadAppointments(): void {
    if (!this.currentUser) {
      return;
    }
    if (this.currentUser.role === 'CLIENT') {
      this.appointmentService.listForClient().subscribe({
        next: data => this.appointments = data,
        error: () => this.statusMessage = 'Unable to load appointments.'
      });
      return;
    }

    if (!this.counselorProfile) {
      return;
    }
    this.appointmentService.listForCounselor(this.counselorProfile.id).subscribe({
      next: data => this.counselorAppointments = data,
      error: () => this.statusMessage = 'Unable to load sessions.'
    });
  }

  get nextAppointment(): Appointment | null {
    if (!this.appointments.length) {
      return null;
    }
    const now = new Date().getTime();
    return [...this.appointments]
      .filter(appt => this.isUpcoming(appt.appointmentDate, appt.endTime))
      .filter(appt => !['CANCELLED', 'EXPIRED', 'COMPLETED'].includes(appt.status))
      .sort((a, b) =>
        `${a.appointmentDate}T${a.startTime}`.localeCompare(`${b.appointmentDate}T${b.startTime}`)
      )[0] || null;
  }

  get nextSession(): CounselorAppointment | null {
    if (!this.counselorAppointments.length) {
      return null;
    }
    return [...this.counselorAppointments]
      .filter(appt => this.isUpcoming(appt.appointmentDate, appt.endTime))
      .filter(appt => !['CANCELLED', 'EXPIRED', 'COMPLETED'].includes(appt.status))
      .sort((a, b) =>
        `${a.appointmentDate}T${a.startTime}`.localeCompare(`${b.appointmentDate}T${b.startTime}`)
      )[0] || null;
  }

  private isUpcoming(date: string, endTime: string): boolean {
    if (!date || !endTime) {
      return false;
    }
    const end = new Date(`${date}T${endTime}`);
    return end.getTime() >= Date.now();
  }
}
