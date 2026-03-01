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

  constructor(private appointmentService: AppointmentService) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.appointmentService.listForClient().subscribe({
      next: data => this.appointments = data,
      error: () => this.message = 'Unable to load appointments.'
    });
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
