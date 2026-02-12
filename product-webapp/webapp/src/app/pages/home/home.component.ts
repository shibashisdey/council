import { Component, OnInit } from '@angular/core';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  topCounselors: CounselorProfile[] = [];
  loading = false;
  errorMessage = '';

  constructor(private counselorService: CounselorService) {}

  ngOnInit(): void {
    this.loadTopCounselors();
  }

  private loadTopCounselors(): void {
    this.loading = true;
    this.errorMessage = '';
    this.counselorService.listActive().subscribe({
      next: data => {
        this.topCounselors = [...data]
          .sort((a, b) => (b.experienceYears || 0) - (a.experienceYears || 0))
          .slice(0, 3);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load counselors right now.';
        this.loading = false;
      }
    });
  }
}
