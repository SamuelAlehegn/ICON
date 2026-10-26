import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BreakpointObserver } from '@angular/cdk/layout';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { IconService } from '../../services/icon.service';
import { IconSummary } from '../../models/icon.model';
import { LazyImageDirective } from '../../directives/lazy-image.directive';

@Component({
  selector: 'app-icons',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    LazyImageDirective,
  ],
  templateUrl: './icons.component.html',
  styleUrl: './icons.component.scss',
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

  filterForm = new FormGroup({
    q: new FormControl(''),
    region: new FormControl(''),
    churchId: new FormControl(''),
    saintId: new FormControl(''),
    category: new FormControl(''),
    sort: new FormControl('newest'),
  });

  regions = [
    'Tigray',
    'Afar',
    'Amhara',
    'Oromia',
    'Gambela',
    'Somali',
    'Benishangul-Gumuz',
    'Harari',
    'Sidama',
    'Central Ethiopia Regional State',
    'South West Ethiopia Peoples',
    'Southern Ethiopia Peoples',
    'Addis Ababa',
    'Dire Dawa',
  ];

  categories = ['saint', 'christ', 'mary', 'trinity', 'cross', 'miracle', 'feast'];

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
        this.loadIcons(0);
      });
  }

  ngOnInit(): void {
    this.loadIcons(0);
  }

  loadIcons(page = this.currentPage): void {
    const requestId = ++this.requestId;
    this.requestedPage = page;
    this.loading = true;
    this.error = false;

    const values = this.filterForm.getRawValue();

    this.iconService
      .getIcons({
        q: values.q?.trim() || undefined,
        region: values.region || undefined,
        churchId: values.churchId?.trim() || undefined,
        saintId: values.saintId?.trim() || undefined,
        category: values.category || undefined,
        sort: values.sort || undefined,
        page,
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          if (requestId !== this.requestId) {
            return;
          }

          this.icons = result.content || [];
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
    this.filterForm.reset({
      q: '',
      region: '',
      churchId: '',
      saintId: '',
      category: '',
      sort: 'newest',
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

  get hasActiveFilters(): boolean {
    const values = this.filterForm.getRawValue();
    return Boolean(
      values.q?.trim() ||
        values.region ||
        values.churchId?.trim() ||
        values.saintId?.trim() ||
        values.category ||
        (values.sort && values.sort !== 'newest')
    );
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
