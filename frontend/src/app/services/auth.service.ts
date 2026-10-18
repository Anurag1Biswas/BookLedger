import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private tokenKey = 'lms_token';
  private roleKey = 'lms_role';
  private usernameKey = 'lms_username';

  isLoggedIn$ = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient, private router: Router) {}

  login(req: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, req).pipe(
      tap(res => {
        localStorage.setItem(this.tokenKey, res.token);
        localStorage.setItem(this.roleKey, res.role);
        localStorage.setItem(this.usernameKey, res.username);
        this.isLoggedIn$.next(true);
      })
    );
  }

  logout(reason?: 'expired') {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.roleKey);
    localStorage.removeItem(this.usernameKey);
    this.isLoggedIn$.next(false);
    this.router.navigate(['/login'], reason ? { queryParams: { reason } } : {});
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRole(): string | null {
    return localStorage.getItem(this.roleKey);
  }

  getUsername(): string | null {
    return localStorage.getItem(this.usernameKey);
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  isLibrarian(): boolean {
    return this.getRole() === 'LIBRARIAN';
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }

  changePassword(oldPassword: string, newPassword: string) {
    return this.http.put(`${this.apiUrl}/change-password`, { oldPassword, newPassword });
  }
}
