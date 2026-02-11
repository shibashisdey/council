import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SessionNote {
  id: number;
  appointmentId: number;
  userId?: number;
  counselorId: number;
  sessionDate: string;
  summary: string;
  observations: string;
  recommendations: string;
  privateNotes?: string;
  sharedWithClient?: boolean;
  pdfUrl?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SessionNoteService {
  private baseUrl = `${environment.apiBaseUrl}/session-notes`;

  constructor(private http: HttpClient) {}

  create(payload: any): Observable<SessionNote> {
    return this.http.post<SessionNote>(this.baseUrl, payload);
  }

  update(noteId: number, payload: any): Observable<SessionNote> {
    return this.http.put<SessionNote>(`${this.baseUrl}/${noteId}`, payload);
  }

  share(noteId: number, payload: any): Observable<SessionNote> {
    return this.http.patch<SessionNote>(`${this.baseUrl}/${noteId}/share`, payload);
  }

  getNotesForUser(userId: number): Observable<SessionNote[]> {
    return this.http.get<SessionNote[]>(`${this.baseUrl}/user/${userId}`);
  }

  getNotesForCounselor(counselorId: number): Observable<SessionNote[]> {
    return this.http.get<SessionNote[]>(`${this.baseUrl}/counselor/${counselorId}`);
  }
}
