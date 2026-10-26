import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { IconService } from '../../services/icon.service';
import { IconSummary } from '../../models/icon.model';
import { LazyImageDirective } from '../../directives/lazy-image.directive';

@Component({
  selector: 'app-icons',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatProgressBarModule, RouterLink, LazyImageDirective],
  templateUrl: './icons.component.html',
  styleUrl: './icons.component.scss'
})
export class IconsComponent implements OnInit {
  private readonly iconService = inject(IconService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly destroyRef = inject(DestroyRef);
  private readonly pageSize = 12;

  @ViewChild('resultsHeading') private resultsHeading?: ElementRef<HTMLHeadingElement>;

  icons: IconSummary[] = [];
  loading = true;
  error = false;
  currentPage = 0;
  totalElements = 0;
  totalPages = 0;
  maxVisiblePages = 5;

  readonly skeletonCards = Array.from({ length: 12 }, (_, index) => index);

  private requestedPage = 0;
  private focusResultsAfterLoad = false;

  constructor() {
    this.breakpointObserver
      .observe('(max-width: 640px)')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => {
        this.maxVisiblePages = state.matches ? 3 : 5;
      });
  }

  ngOnInit(): void {
    this.loadIcons(0);
  }

  loadIcons(page = this.currentPage): void {
    this.requestedPage = page;
    this.loading = true;
    this.error = false;
    this.iconService.getIcons({ page, size: this.pageSize }).subscribe({
      next: (result) => {
        this.icons = result.content || [];
        this.currentPage = result.page ?? page;
        this.totalElements = result.totalElements;
        this.totalPages = result.totalPages;
        this.loading = false;
        this.focusResultsIfNeeded();
      },
      error: () => {
        this.error = true;
        this.loading = false;
        this.focusResultsAfterLoad = false;
      }
    });
  }

  retry(): void {
    this.loadIcons(this.requestedPage);
  }

  goToPage(page: number): void {
    if (this.loading || this.totalPages === 0) {
      return;
    }

    const target = Math.min(Math.max(page, 0), this.totalPages - 1);
    if (target === this.currentPage) {
      return;
    }

    this.focusResultsAfterLoad = true;
    this.loadIcons(target);
  }

  onPaginationKeydown(event: KeyboardEvent): void {
    const handledKeys = ['ArrowLeft', 'ArrowRight', 'Home', 'End'];
    if (!handledKeys.includes(event.key)) {
      return;
    }

    const container = event.currentTarget as HTMLElement | null;
    const buttons = Array.from(container?.querySelectorAll<HTMLButtonElement>('.page-button') ?? []);
    if (buttons.length === 0) {
      return;
    }

    const focusedIndex = buttons.indexOf(document.activeElement as HTMLButtonElement);
    const currentIndex = buttons.findIndex((button) => button.getAttribute('aria-current') === 'page');
    const startIndex = focusedIndex >= 0 ? focusedIndex : Math.max(currentIndex, 0);

    let nextIndex = startIndex;
    switch (event.key) {
      case 'ArrowLeft':
        nextIndex = Math.max(0, startIndex - 1);
        break;
      case 'ArrowRight':
        nextIndex = Math.min(buttons.length - 1, startIndex + 1);
        break;
      case 'Home':
        nextIndex = 0;
        break;
      case 'End':
        nextIndex = buttons.length - 1;
        break;
    }

    event.preventDefault();
    buttons[nextIndex].focus();
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
    if (this.totalPages <= this.maxVisiblePages) {
      return Array.from({ length: this.totalPages }, (_, index) => index);
    }

    let start = Math.max(0, this.currentPage - Math.floor(this.maxVisiblePages / 2));
    const end = Math.min(this.totalPages, start + this.maxVisiblePages);
    start = Math.max(0, end - this.maxVisiblePages);

    return Array.from({ length: end - start }, (_, index) => start + index);
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

  getLocation(icon: IconSummary): string {
    const parts = [icon.town, icon.region, icon.country].filter((p) => !!p);
    return parts.length > 0 ? parts.join(', ') : '—';
  }

  private focusResultsIfNeeded(): void {
    if (!this.focusResultsAfterLoad) {
      return;
    }

    this.focusResultsAfterLoad = false;
    setTimeout(() => this.resultsHeading?.nativeElement.focus());
  }
}
