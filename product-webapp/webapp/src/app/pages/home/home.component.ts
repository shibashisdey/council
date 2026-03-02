import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStateService } from '../../core/auth-state.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  constructor(private authState: AuthStateService, private router: Router) {}

  ngOnInit(): void {
    if (this.authState.currentUser) {
      this.router.navigate(['/dashboard']);
    }
  }
}
