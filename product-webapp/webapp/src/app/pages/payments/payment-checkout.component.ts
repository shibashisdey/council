import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService, PaymentResponse } from '../../services/payment.service';
import { Appointment, AppointmentService } from '../../services/appointment.service';

@Component({
  selector: 'app-payment-checkout',
  templateUrl: './payment-checkout.component.html',
  styleUrls: ['./payment-checkout.component.css']
})
export class PaymentCheckoutComponent implements OnInit {
  appointmentId = 0;
  amount = 0;
  counselorName = '';
  loading = false;
  message = '';
  error = '';
  payment: PaymentResponse | null = null;
  meetingLink = '';
  appointment: Appointment | null = null;

  constructor(
    private route: ActivatedRoute,
    private paymentService: PaymentService,
    private router: Router,
    private appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    this.appointmentId = Number(this.route.snapshot.paramMap.get('appointmentId'));
    this.amount = Number(this.route.snapshot.queryParamMap.get('amount')) || 0;
    this.counselorName = this.route.snapshot.queryParamMap.get('counselor') || '';

    if (!this.appointmentId) {
      this.error = 'Missing appointment information.';
      return;
    }
    this.createPayment();
  }

  private createPayment(): void {
    this.loading = true;
    this.paymentService.createPayment({
      appointmentId: this.appointmentId,
      amount: this.amount
    }).subscribe({
      next: response => {
        this.payment = response;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = typeof err?.error === 'string'
          ? err.error
          : (err?.error?.message || 'Unable to initiate payment.');
      }
    });
  }

  payNow(): void {
    if (!this.appointmentId) {
      return;
    }
    this.loading = true;
    this.paymentService.simulateSuccess(this.appointmentId).subscribe({
      next: () => {
        this.loading = false;
        this.message = 'Payment successful. Appointment confirmed.';
        this.fetchMeetingLink();
      },
      error: err => {
        this.loading = false;
        this.error = typeof err?.error === 'string'
          ? err.error
          : (err?.error?.message || 'Payment failed.');
      }
    });
  }

  private fetchMeetingLink(): void {
    this.appointmentService.listForClient().subscribe({
      next: appointments => {
        const match = appointments.find(item => item.appointmentId === this.appointmentId);
        this.appointment = match || null;
        this.meetingLink = match?.meetingLink || '';
      },
      error: () => {
        // Keep silent; user can still navigate to appointments page.
      }
    });
  }

  goToAppointments(): void {
    this.router.navigate(['/appointments']);
  }
}
