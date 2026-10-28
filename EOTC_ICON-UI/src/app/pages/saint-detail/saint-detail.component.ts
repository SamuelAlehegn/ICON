import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChurchSummary, IconSummary, SaintDetail } from '../../models/icon.model';
import { SaintService } from '../../services/saint.service';
import { LazyImageDirective } from '../../directives/lazy-image.directive';

@Component({
  selector: 'app-saint-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    LazyImageDirective,
  ],
  templateUrl: './saint-detail.component.html',
  styleUrl: './saint-detail.component.scss',
})
export class SaintDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly saintService = inject(SaintService);
  private readonly destroyRef = inject(DestroyRef);

  saint: SaintDetail | null = null;
  loading = true;
  error = false;
  notFound = false;

  private currentId = '';
  private requestId = 0;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = params.get('id');
      if (!id) {
        this.requestId += 1;
        this.saint = null;
        this.notFound = true;
        this.loading = false;
        return;
      }

      this.loadSaint(id);
    });
  }

  loadSaint(idOrSlug: string): void {
    const requestId = ++this.requestId;
    this.currentId = idOrSlug;
    this.loading = true;
    this.error = false;
    this.notFound = false;

    this.saintService.getSaint(idOrSlug).subscribe({
      next: (saint) => {
        if (requestId !== this.requestId) {
          return;
        }

        this.saint = saint;
        this.loading = false;
      },
      error: (error: unknown) => {
        if (requestId !== this.requestId) {
          return;
        }

        this.saint = null;
        this.loading = false;
        this.notFound = error instanceof HttpErrorResponse && error.status === 404;
        this.error = !this.notFound;
      },
    });
  }

  retry(): void {
    if (this.currentId) {
      this.loadSaint(this.currentId);
    }
  }

  get category(): string {
    return this.saint?.category?.trim() || 'Uncategorized';
  }

  get alternativeNames(): string[] {
    return (this.saint?.aliases ?? []).map((name) => name.trim()).filter(Boolean);
  }

  get verifiedDescription(): string | null {
    if (!this.saint?.verified) {
      return null;
    }
    return this.saint.description?.trim() || null;
  }

  get depictions(): IconSummary[] {
    return this.saint?.depictions ?? [];
  }

  get churches(): ChurchSummary[] {
    return this.saint?.churches ?? [];
  }

  get relationCountLabel(): string {
    const count = this.saint?.iconCount ?? this.depictions.length;
    return count === 1 ? '1 related icon' : `${count} related icons`;
  }

  getDisplayName(icon: IconSummary): string {
    if (icon.saintNames && icon.saintNames.length > 0) {
      return icon.saintNames.join(', ');
    }
    return icon.depictedFigure || icon.title || 'Untitled icon';
  }

  getImageAlt(icon: IconSummary): string {
    return icon.altText?.trim() || `Icon depicting ${this.getDisplayName(icon)}`;
  }

  getIconLocation(icon: IconSummary): string {
    const parts = [icon.town, icon.region, icon.country].filter((part) => !!part);
    return parts.length > 0 ? parts.join(', ') : 'Location not recorded';
  }

  getChurchLocation(church: ChurchSummary): string {
    const parts = [church.town, church.region, church.country].filter((part) => !!part);
    return parts.length > 0 ? parts.join(', ') : 'Location not recorded';
  }
}
