import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';
import { AvailabilityBlock, AvailabilityService } from '../../services/availability.service';
import { AppointmentService } from '../../services/appointment.service';

@Component({
  selector: 'app-counselor-detail',
  templateUrl: './counselor-detail.component.html',
  styleUrls: ['./counselor-detail.component.css']
})
export class CounselorDetailComponent implements OnInit {
  counselor: CounselorProfile | null = null;
  appointmentDate = '';
  slots: AvailabilityBlock[] = [];
  selectedSlot: AvailabilityBlock | null = null;
  loadingSlots = false;
  message = '';
  error = '';

  constructor(
    private route: ActivatedRoute,
    private counselorService: CounselorService,
    private availabilityService: AvailabilityService,
    private appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.counselorService.getById(id).subscribe({
      next: data => this.counselor = data,
      error: () => this.error = 'Unable to load counselor.'
    });
  }

  onDateChange(): void {
    this.selectedSlot = null;
    this.slots = [];
    this.message = '';
    this.error = '';
    if (!this.counselor || !this.appointmentDate) {
      return;
    }
    this.loadingSlots = true;
    this.availabilityService.getCalendar(this.counselor.id, this.appointmentDate).subscribe({
      next: slots => {
        this.slots = slots || [];
        this.loadingSlots = false;
      },
      error: () => {
        this.loadingSlots = false;
        this.error = 'Unable to load slots.';
      }
    });
  }

  selectSlot(slot: AvailabilityBlock): void {
    if (slot.status !== 'AVAILABLE') {
      return;
    }
    this.selectedSlot = slot;
  }

  formatTime(value: string): string {
    return value && value.length >= 5 ? value.substring(0, 5) : value;
  }

  book(): void {
    this.message = '';
    this.error = '';
    if (!this.counselor || !this.selectedSlot) {
      this.error = 'Select a date and an available slot first.';
      return;
    }
    this.appointmentService.create({
      counselorId: this.counselor.id,
      appointmentDate: this.appointmentDate,
      startTime: this.selectedSlot.startTime
    }).subscribe({
      next: () => {
        this.message = 'Appointment booked successfully.';
      },
      error: (err) => {
        this.error = err?.error?.message || 'Unable to book appointment.';
      }
    });
  }
}
