import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { moderatorGuard } from './guards/role.guard';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in — EOTC Icon Archive',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    title: 'Create an account — EOTC Icon Archive',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
      { path: 'icons', loadComponent: () => import('./pages/icons/icons.component').then(m => m.IconsComponent) },
      { path: 'icons/:id', loadComponent: () => import('./pages/icons/icon-detail/icon-detail.component').then(m => m.IconDetailComponent) },
      { path: 'saints', loadComponent: () => import('./pages/saints/saints.component').then(m => m.SaintsComponent) },
      { path: 'saints/:id', loadComponent: () => import('./pages/saints/saint-detail/saint-detail.component').then(m => m.SaintDetailComponent) },
      { path: 'learn-iconography', loadComponent: () => import('./pages/learn-iconography/learn-iconography.component').then(m => m.LearnIconographyComponent) },
      { path: 'resources', loadComponent: () => import('./pages/learn-iconography/resources/resources.component').then(m => m.ResourcesComponent) },
      { path: 'contribute', loadComponent: () => import('./pages/contribute/contribute.component').then(m => m.ContributeComponent) },
      { path: 'contribute/success', loadComponent: () => import('./pages/contribute/contribute-success/contribute-success.component').then(m => m.ContributeSuccessComponent) },
      { path: 'churches', loadComponent: () => import('./pages/churches/churches.component').then(m => m.ChurchesComponent) },
      { path: 'churches/:id', loadComponent: () => import('./pages/churches/church-detail/church-detail.component').then(m => m.ChurchDetailComponent) },
      { path: 'map', loadComponent: () => import('./pages/map/map.component').then(m => m.MapComponent) },
      { path: 'account', canActivate: [authGuard], loadComponent: () => import('./pages/account/account.component').then(m => m.AccountComponent) },
      {
        path: 'admin',
        title: 'Moderation queue — EOTC Icon Archive',
        canActivate: [moderatorGuard],
        loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent)
      },
      {
        path: 'admin/submissions/:id',
        title: 'Review submission — EOTC Icon Archive',
        canActivate: [moderatorGuard],
        loadComponent: () => import('./pages/admin/submission-review/submission-review.component').then(m => m.AdminSubmissionReviewComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];

