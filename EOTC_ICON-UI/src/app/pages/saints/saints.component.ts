import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BreakpointObserver } from '@angular/cdk/layout';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { SaintSummary } from '../../models/saint.model';
import { SaintService } from '../../services/saint.service';

@Component({
  selector: 'app-saints',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    RouterLink,
  ],
  templateUrl: './saints.component.html',
  styleUrl: './saints.component.scss',
})
export class SaintsComponent implements OnInit {
  private readonly saintService = inject(SaintService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly destroyRef = inject(DestroyRef);
  private readonly pageSize = 12;

  @ViewChild('resultsHeading') private resultsHeading?: ElementRef<HTMLHeadingElement>;

  saints: SaintSummary[] = [];
  loading = true;
  error = false;
  currentPage = 0;
  totalElements = 0;
  totalPages = 0;
  maxVisiblePages = 5;

  readonly skeletonCards = Array.from({ length: 12 }, (_, index) => index);

  filterForm = new FormGroup({
    q: new FormControl(''),
  });

  private requestedPage = 0;
  private requestId = 0;
  private focusResultsAfterLoad = false;

  constructor() {
    this.breakpointObserver
      .observe('(max-width: 640px)')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => {
        this.maxVisiblePages = state.matches ? 3 : 5;
      });

    this.filterForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.focusResultsAfterLoad = false;
        this.loadSaints(0);
      });
  }

  ngOnInit(): void {
    this.loadSaints(0);
  }

  loadSaints(page = this.currentPage): void {
    const requestId = ++this.requestId;
    this.requestedPage = page;
    this.loading = true;
    this.error = false;

    const query = this.filterForm.getRawValue().q?.trim() || undefined;

    this.saintService
      .getSaints({
        q: query,
        sort: 'name',
        page,
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          if (requestId !== this.requestId) {
            return;
          }

          this.saints = result.content || [];
          this.currentPage = result.page ?? page;
          this.totalElements = result.totalElements;
          this.totalPages = result.totalPages;
          this.loading = false;
          this.focusResultsIfNeeded();
        },
        error: () => {
          if (requestId !== this.requestId) {
            return;
          }

          this.error = true;
          this.loading = false;
          this.focusResultsAfterLoad = false;
        },
      });
  }

  clearFilters(): void {
    this.filterForm.reset({ q: '' });
  }

  retry(): void {
    this.loadSaints(this.requestedPage);
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
    this.loadSaints(target);
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

  get hasActiveFilters(): boolean {
    return Boolean(this.filterForm.getRawValue().q?.trim());
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

  getCategory(saint: SaintSummary): string {
    return saint.category?.trim() || 'Uncategorized';
  }

  getIconCount(saint: SaintSummary): string {
    if (saint.iconCount === null || saint.iconCount === undefined) {
      return '—';
    }
    return saint.iconCount === 1 ? '1 icon' : `${saint.iconCount} icons`;
  }

  private focusResultsIfNeeded(): void {
    if (!this.focusResultsAfterLoad) {
      return;
    }

    this.focusResultsAfterLoad = false;
    setTimeout(() => this.resultsHeading?.nativeElement.focus());
  }
}
