import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';
  loading = false;
  showPassword = false;
  sessionExpired = false;

  constructor(private auth: AuthService, private router: Router, route: ActivatedRoute) {
    this.sessionExpired = route.snapshot.queryParamMap.get('reason') === 'expired';
    if (this.auth.getToken()) this.router.navigate(['/dashboard']);
  }

  login() {
    if (!this.username.trim() || !this.password) {
      this.error = 'Enter your username and password.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.sessionExpired = false;
    this.auth.login({ username: this.username.trim(), password: this.password }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.status === 0
          ? 'Cannot reach the server. Please try again in a moment.'
          : err.status === 401
            ? 'Invalid username or password. Please try again.'
            : 'Sign-in failed because of a server problem. Please try again shortly.';
      }
    });
  }
}
