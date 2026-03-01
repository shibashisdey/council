import { Component, OnInit } from '@angular/core';
import { AppointmentService, CounselorAppointment } from '../../services/appointment.service';
import { CounselorService } from '../../services/counselor.service';

@Component({
  selector: 'app-counselor-sessions',
  templateUrl: './counselor-sessions.component.html',
  styleUrls: ['./counselor-sessions.component.css']
})
export class CounselorSessionsComponent implements OnInit {
  sessions: CounselorAppointment[] = [];
  message = '';

  constructor(
    private appointmentService: AppointmentService,
    private counselorService: CounselorService
  ) {}

  ngOnInit(): void {
    this.loadSessions();
  }

  private loadSessions(): void {
    this.counselorService.getMe().subscribe({
      next: profile => {
        this.appointmentService.listForCounselor(profile.id).subscribe({
          next: data => this.sessions = data,
          error: () => this.message = 'Unable to load sessions.'
        });
      },
      error: () => this.message = 'Unable to load counselor profile.'
    });
  }
}
