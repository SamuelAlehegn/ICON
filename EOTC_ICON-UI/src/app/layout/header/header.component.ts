import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface NavItem {
  label: string;
  path: string;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatMenuModule
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly navItems: NavItem[] = [
    { label: 'Home', path: '/' },
    { label: 'Icons', path: '/icons' },
    { label: 'Churches', path: '/churches' },
    { label: 'Saints', path: '/saints' },
    { label: 'Map', path: '/map' },
    { label: 'Learn Iconography', path: '/learn-iconography' }
  ];

  readonly isAuthenticated = this.auth.isAuthenticated;
  readonly isModerator = this.auth.isModerator;

  signOut(): void {
    this.auth.logout().subscribe(() => void this.router.navigateByUrl('/'));
  }
}
