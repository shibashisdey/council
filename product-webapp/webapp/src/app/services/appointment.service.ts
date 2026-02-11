import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Appointment {
  appointmentId: number;
  clientId: number;
  counselorId: number;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: string;
  paymentId?: string;
  createdAt?: string;
}

export interface CounselorAppointment {
  appointmentId: number;
  clientId: number;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private baseUrl = `${environment.apiBaseUrl}/appointments`;

  constructor(private http: HttpClient) {}

  create(payload: any): Observable<Appointment> {
    return this.http.post<Appointment>(this.baseUrl, payload);
  }

  listForClient(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.baseUrl}/client`);
  }

  listForCounselor(counselorId: number): Observable<CounselorAppointment[]> {
    return this.http.get<CounselorAppointment[]>(`${this.baseUrl}/counselor/${counselorId}`);
  }

  reschedule(appointmentId: number, payload: any): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/${appointmentId}/reschedule`, payload);
  }

  cancel(appointmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${appointmentId}`);
  }
}
