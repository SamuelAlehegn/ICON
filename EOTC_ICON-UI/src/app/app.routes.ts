import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
      { path: 'icons', loadComponent: () => import('./pages/icons/icons.component').then(m => m.IconsComponent) },
      { path: 'learn-iconography', loadComponent: () => import('./pages/learn-iconography/learn-iconography.component').then(m => m.LearnIconographyComponent) },
      { path: 'resources', loadComponent: () => import('./pages/resources/resources.component').then(m => m.ResourcesComponent) }
    ]
  },
  { path: '**', redirectTo: '' }
];
