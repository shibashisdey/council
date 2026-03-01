import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {

  user = {
    email: '',
    password: '',
    role: 'CLIENT'
  };

  errorMessage = '';
  successMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit(): void {
  }

  register(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.loading = true;
    this.authService.register(this.user).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Registration successful. Please sign in.';
        setTimeout(() => this.router.navigate(['/auth/login']), 800);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Registration failed. Try a different email.';
      }
    });
  }
}
