import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Review {
  id: number;
  appointmentId: number;
  userId: number;
  counselorId: number;
  rating: number;
  comment: string;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private baseUrl = `${environment.apiBaseUrl}/reviews`;

  constructor(private http: HttpClient) {}

  createReview(payload: any): Observable<Review> {
    return this.http.post<Review>(this.baseUrl, payload);
  }

  getReviewsForCounselor(counselorId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.baseUrl}/counselor/${counselorId}`);
  }

  getReviewsForUser(userId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.baseUrl}/user/${userId}`);
  }
}
