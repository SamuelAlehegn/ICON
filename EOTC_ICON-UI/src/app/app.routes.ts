import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
      { path: 'icons', loadComponent: () => import('./pages/icons/icons.component').then(m => m.IconsComponent) },
      { path: 'icons/:id', loadComponent: () => import('./pages/icon-detail/icon-detail.component').then(m => m.IconDetailComponent) },
      { path: 'saints', loadComponent: () => import('./pages/saints/saints.component').then(m => m.SaintsComponent) },
      { path: 'saints/:id', loadComponent: () => import('./pages/saint-detail/saint-detail.component').then(m => m.SaintDetailComponent) },
      { path: 'learn-iconography', loadComponent: () => import('./pages/learn-iconography/learn-iconography.component').then(m => m.LearnIconographyComponent) },
      { path: 'resources', loadComponent: () => import('./pages/resources/resources.component').then(m => m.ResourcesComponent) },
      { path: 'contribute', loadComponent: () => import('./pages/contribute/contribute.component').then(m => m.ContributeComponent) },
      { path: 'contribute/success', loadComponent: () => import('./pages/contribute-success/contribute-success.component').then(m => m.ContributeSuccessComponent) },
      { path: 'churches', loadComponent: () => import('./pages/churches/churches.component').then(m => m.ChurchesComponent) },
      { path: 'churches/:id', loadComponent: () => import('./pages/church-detail/church-detail.component').then(m => m.ChurchDetailComponent) }
    ]
  },
  { path: '**', redirectTo: '' }
];

