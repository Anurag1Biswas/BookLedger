import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./components/shared/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard]
  },

  // ---- ADMIN ONLY routes ----
  {
    path: 'members',
    loadComponent: () => import('./components/members/members.component').then(m => m.MembersComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'ADMIN' }
  },

  // ---- LIBRARIAN ONLY routes ----
  {
    path: 'books',
    loadComponent: () => import('./components/books/books.component').then(m => m.BooksComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'LIBRARIAN' }
  },
  {
    path: 'issue',
    loadComponent: () => import('./components/librarian/issue/issue.component').then(m => m.IssueComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'LIBRARIAN' }
  },
  {
    path: 'submit',
    loadComponent: () => import('./components/librarian/submit/submit.component').then(m => m.SubmitComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'LIBRARIAN' }
  },
  {
    path: 'transactions',
    loadComponent: () => import('./components/librarian/transactions/transactions.component').then(m => m.TransactionsComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'LIBRARIAN' }
  },

  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
