import { Component, OnInit } from '@angular/core';
import { Appointment, AppointmentService } from '../../services/appointment.service';

@Component({
  selector: 'app-client-appointments',
  templateUrl: './client-appointments.component.html',
  styleUrls: ['./client-appointments.component.css']
})
export class ClientAppointmentsComponent implements OnInit {
  appointments: Appointment[] = [];
  message = '';
  showPast = false;

  constructor(private appointmentService: AppointmentService) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.appointmentService.listForClient().subscribe({
      next: data => this.appointments = data || [],
      error: () => this.message = 'Unable to load appointments.'
    });
  }

  get upcomingAppointments(): Appointment[] {
    const now = new Date();
    return this.appointments
      .filter(appt => {
        const end = new Date(`${appt.appointmentDate}T${appt.endTime}`);
        return end.getTime() >= now.getTime();
      })
      .sort((a, b) =>
        `${a.appointmentDate}T${a.startTime}`.localeCompare(`${b.appointmentDate}T${b.startTime}`)
      );
  }

  get pastAppointments(): Appointment[] {
    const now = new Date();
    return this.appointments
      .filter(appt => {
        const end = new Date(`${appt.appointmentDate}T${appt.endTime}`);
        return end.getTime() < now.getTime();
      })
      .sort((a, b) =>
        `${b.appointmentDate}T${b.startTime}`.localeCompare(`${a.appointmentDate}T${a.startTime}`)
      );
  }

  cancel(appointmentId: number): void {
    this.appointmentService.cancel(appointmentId).subscribe({
      next: () => {
        this.message = 'Appointment cancelled.';
        this.loadAppointments();
      },
      error: () => {
        this.message = 'Unable to cancel appointment.';
      }
    });
  }
}
