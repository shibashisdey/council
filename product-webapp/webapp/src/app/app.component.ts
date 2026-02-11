import { Component } from '@angular/core';
import { AuthStateService } from './core/auth-state.service';
import { AuthService } from './auth/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'StackFul Minds';

  constructor(public authState: AuthStateService, private authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
