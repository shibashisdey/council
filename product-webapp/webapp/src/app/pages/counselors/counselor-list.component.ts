import { Component, OnInit } from '@angular/core';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-counselor-list',
  templateUrl: './counselor-list.component.html',
  styleUrls: ['./counselor-list.component.css']
})
export class CounselorListComponent implements OnInit {
  counselors: CounselorProfile[] = [];
  loading = false;
  errorMessage = '';

  constructor(private counselorService: CounselorService, private router: Router) {}

  ngOnInit(): void {
    this.loadCounselors();
  }

  loadCounselors(): void {
    this.loading = true;
    this.errorMessage = '';
    this.counselorService.listActive().subscribe({
      next: data => {
        this.counselors = data;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load counselors right now.';
        this.loading = false;
      }
    });
  }

  viewCounselor(id: number): void {
    this.router.navigate(['/counselors', id]);
  }
}
