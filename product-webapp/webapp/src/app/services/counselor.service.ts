import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CounselorProfile {
  id: number;
  userId: number;
  fullName: string;
  qualification: string;
  experienceYears: number;
  bio: string;
  pricePerSession: number;
  active: boolean;
  specializations: string[];
}

@Injectable({
  providedIn: 'root'
})
export class CounselorService {
  private baseUrl = `${environment.apiBaseUrl}/counselors`;

  constructor(private http: HttpClient) {}

  getMe(): Observable<CounselorProfile> {
    return this.http.get<CounselorProfile>(`${this.baseUrl}/me`);
  }

  createProfile(payload: any): Observable<CounselorProfile> {
    return this.http.post<CounselorProfile>(this.baseUrl, payload);
  }

  updateProfile(id: number, payload: any): Observable<CounselorProfile> {
    return this.http.put<CounselorProfile>(`${this.baseUrl}/${id}`, payload);
  }

  listActive(): Observable<CounselorProfile[]> {
    return this.http.get<CounselorProfile[]>(this.baseUrl);
  }

  getById(id: number): Observable<CounselorProfile> {
    return this.http.get<CounselorProfile>(`${this.baseUrl}/${id}`);
  }
}
