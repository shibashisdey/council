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
  showPast = false;

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
          next: data => this.sessions = data || [],
          error: () => this.message = 'Unable to load sessions.'
        });
      },
      error: () => this.message = 'Unable to load counselor profile.'
    });
  }

  get upcomingSessions(): CounselorAppointment[] {
    const now = new Date();
    return this.sessions
      .filter(appt => {
        const end = new Date(`${appt.appointmentDate}T${appt.endTime}`);
        return end.getTime() >= now.getTime();
      })
      .sort((a, b) =>
        `${a.appointmentDate}T${a.startTime}`.localeCompare(`${b.appointmentDate}T${b.startTime}`)
      );
  }

  get pastSessions(): CounselorAppointment[] {
    const now = new Date();
    return this.sessions
      .filter(appt => {
        const end = new Date(`${appt.appointmentDate}T${appt.endTime}`);
        return end.getTime() < now.getTime();
      })
      .sort((a, b) =>
        `${b.appointmentDate}T${b.startTime}`.localeCompare(`${a.appointmentDate}T${a.startTime}`)
      );
  }
}
