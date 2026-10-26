import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { IconService } from '../../services/icon.service';
import { IconSummary } from '../../models/icon.model';

@Component({
  selector: 'app-icons',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './icons.component.html',
  styleUrl: './icons.component.scss'
})
export class IconsComponent implements OnInit {
  private readonly iconService = inject(IconService);

  icons: IconSummary[] = [];
  loading = true;
  error = false;

  ngOnInit(): void {
    this.loadIcons();
  }

  loadIcons(): void {
    this.loading = true;
    this.error = false;
    this.iconService.getIcons({ size: 24 }).subscribe({
      next: (page) => {
        this.icons = page.content || [];
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  getDisplayName(icon: IconSummary): string {
    if (icon.saintNames && icon.saintNames.length > 0) {
      return icon.saintNames.join(', ');
    }
    return icon.depictedFigure || icon.title || 'Unknown';
  }

  getLocation(icon: IconSummary): string {
    const parts = [icon.town, icon.region, icon.country].filter((p) => !!p);
    return parts.length > 0 ? parts.join(', ') : '—';
  }
}
