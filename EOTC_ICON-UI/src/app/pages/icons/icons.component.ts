import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { IconService } from '../../services/icon.service';
import { IconSummary } from '../../models/icon.model';
import { LazyImageDirective } from '../../directives/lazy-image.directive';

@Component({
  selector: 'app-icons',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule, RouterLink, LazyImageDirective],
  templateUrl: './icons.component.html',
  styleUrl: './icons.component.scss'
})
export class IconsComponent implements OnInit {
  private readonly iconService = inject(IconService);
  private readonly pageSize = 12;

  icons: IconSummary[] = [];
  loading = true;
  error = false;
  currentPage = 0;
  totalElements = 0;
  totalPages = 0;

  ngOnInit(): void {
    this.loadIcons(0);
  }

  loadIcons(page = this.currentPage): void {
    this.loading = true;
    this.error = false;
    this.iconService.getIcons({ page, size: this.pageSize }).subscribe({
      next: (result) => {
        this.icons = result.content || [];
        this.currentPage = result.page ?? page;
        this.totalElements = result.totalElements;
        this.totalPages = result.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  goToPage(page: number): void {
    if (this.loading || this.totalPages === 0) {
      return;
    }

    const target = Math.min(Math.max(page, 0), this.totalPages - 1);
    if (target === this.currentPage) {
      return;
    }

    this.loadIcons(target);
  }

  get pageStart(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get pageEnd(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  get hasPrevious(): boolean {
    return this.currentPage > 0;
  }

  get hasNext(): boolean {
    return this.currentPage < this.totalPages - 1;
  }

  get visiblePages(): number[] {
    const maxVisible = 5;
    if (this.totalPages <= maxVisible) {
      return Array.from({ length: this.totalPages }, (_, index) => index);
    }

    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    const end = Math.min(this.totalPages, start + maxVisible);
    start = Math.max(0, end - maxVisible);

    return Array.from({ length: end - start }, (_, index) => start + index);
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
