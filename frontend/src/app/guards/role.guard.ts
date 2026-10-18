import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRole = route.data['role'] as string;
    const userRole = this.auth.getRole();

    if (userRole === requiredRole) {
      return true;
    }

    // Redirect to dashboard with an unauthorised flag
    this.router.navigate(['/dashboard'], { queryParams: { denied: true } });
    return false;
  }
}
