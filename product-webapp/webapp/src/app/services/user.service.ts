import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserProfile {
  userId: number;
  fullName: string;
  age?: number;
  gender?: string;
  phoneNumber?: string;
  city?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl = `${environment.apiBaseUrl}/users`;

  constructor(private http: HttpClient) {}

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/me`);
  }

  createProfile(payload: any): Observable<UserProfile> {
    return this.http.post<UserProfile>(this.baseUrl, payload);
  }

  updateMe(payload: any): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.baseUrl}/me`, payload);
  }
}
