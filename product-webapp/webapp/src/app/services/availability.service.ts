import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AvailabilityBlock {
  date: string;
  startTime: string;
  endTime: string;
  status: string;
  reason: string;
}

@Injectable({
  providedIn: 'root'
})
export class AvailabilityService {
  private baseUrl = `${environment.apiBaseUrl}/schedule`;

  constructor(private http: HttpClient) {}

  getCalendar(counselorId: number, date: string): Observable<AvailabilityBlock[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<AvailabilityBlock[]>(`${this.baseUrl}/calendar/${counselorId}`, { params });
  }

  setWorkingHours(counselorId: number, payload: any): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/working-hours/${counselorId}`, payload);
  }

  setLunchBreak(counselorId: number, payload: any): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/lunch-break/${counselorId}`, payload);
  }

  addUnavailability(counselorId: number, payload: any): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/unavailability/${counselorId}`, payload);
  }

  cancelUnavailability(counselorId: number, unavailabilityId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/unavailability/${counselorId}/${unavailabilityId}`);
  }
}
