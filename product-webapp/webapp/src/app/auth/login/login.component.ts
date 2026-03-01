import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  credentials = {
    identifier: '',
    password: ''
  };

  errorMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit(): void {
  }

  login(): void {
    this.errorMessage = '';
    this.loading = true;
    this.authService.login({
      email: this.credentials.identifier,
      password: this.credentials.password
    }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/setup/profile']);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Invalid email or password.';
      }
    });
  }
}
