import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { AdminSubmissionSummary } from '../../models/admin.model';
import { SubmissionStatus } from '../../models/submission.model';
import { AdminService } from '../../services/admin.service';

const STATUS_LABELS: Record<SubmissionStatus, string> = {
  PENDING: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  NEEDS_CORRECTION: 'Needs correction',
  ARCHIVED: 'Archived'
};

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly pageSize = 10;

  readonly statusOptions: { value: SubmissionStatus; label: string }[] = [
    { value: 'PENDING', label: 'Pending review' },
    { value: 'NEEDS_CORRECTION', label: 'Needs correction' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'ARCHIVED', label: 'Archived' }
  ];

  readonly filterForm = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    status: new FormControl<SubmissionStatus>('PENDING', { nonNullable: true })
  });

  submissions: AdminSubmissionSummary[] = [];
  loading = true;
  error = '';
  currentPage = 0;
  totalElements = 0;
  totalPages = 0;

  private requestedPage = 0;
  private requestId = 0;

  constructor() {
    this.filterForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadSubmissions(0));
  }

  ngOnInit(): void {
    this.loadSubmissions(0);
  }

  loadSubmissions(page = this.currentPage): void {
    const requestId = ++this.requestId;
    this.requestedPage = page;
    this.loading = true;
    this.error = '';

    const values = this.filterForm.getRawValue();
    this.adminService
      .getQueue({
        q: values.q.trim() || undefined,
        status: values.status,
        page,
        size: this.pageSize
      })
      .subscribe({
        next: (result) => {
          if (requestId !== this.requestId) {
            return;
          }
          this.submissions = result.content ?? [];
          this.currentPage = result.page ?? page;
          this.totalElements = result.totalElements;
          this.totalPages = result.totalPages;
          this.loading = false;
        },
        error: (error: unknown) => {
          if (requestId !== this.requestId) {
            return;
          }
          this.error = this.errorMessage(error, 'Unable to load the moderation queue.');
          this.loading = false;
        }
      });
  }

  retry(): void {
    this.loadSubmissions(this.requestedPage);
  }

  goToPage(page: number): void {
    if (this.loading || this.totalPages === 0) {
      return;
    }
    const target = Math.min(Math.max(page, 0), this.totalPages - 1);
    if (target !== this.currentPage) {
      this.loadSubmissions(target);
    }
  }

  statusLabel(status: SubmissionStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  statusClass(status: SubmissionStatus): string {
    return `status-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  get hasPrevious(): boolean {
    return this.currentPage > 0;
  }

  get hasNext(): boolean {
    return this.currentPage < this.totalPages - 1;
  }

  get pageStart(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get pageEnd(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
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

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Please sign in to access the moderation queue.';
      }
      if (error.status === 403) {
        return 'You do not have permission to review submissions.';
      }
      const message = (error.error as { message?: unknown } | null)?.message;
      if (typeof message === 'string' && message.trim()) {
        return message;
      }
    }
    return fallback;
  }
}
