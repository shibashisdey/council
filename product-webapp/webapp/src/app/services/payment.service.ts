import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PaymentResponse {
  paymentId: number;
  appointmentId: number;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private baseUrl = `${environment.apiBaseUrl}/payments`;

  constructor(private http: HttpClient) {}

  createPayment(payload: any): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(this.baseUrl, payload);
  }

  simulateSuccess(appointmentId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${appointmentId}/simulate-success`, {});
  }
}
