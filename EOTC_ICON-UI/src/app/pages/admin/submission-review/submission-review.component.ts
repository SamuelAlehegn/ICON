import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminSubmissionDetail, ReviewAction } from '../../../models/admin.model';
import { IconImage, IconSubject } from '../../../models/icon.model';
import { SubmissionStatus } from '../../../models/submission.model';
import { AdminService } from '../../../services/admin.service';

const STATUS_LABELS: Record<SubmissionStatus, string> = {
  PENDING: 'Pending review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  NEEDS_CORRECTION: 'Needs correction',
  ARCHIVED: 'Archived'
};

const ACTION_LABELS: Record<string, string> = {
  SUBMIT: 'Submitted',
  APPROVE: 'Approved',
  REJECT: 'Rejected',
  REQUEST_CORRECTION: 'Correction requested',
  COMMENT: 'Updated',
  WITHDRAW: 'Removal requested'
};

interface DecisionAction {
  value: ReviewAction;
  label: string;
  icon: string;
  description: string;
}

@Component({
  selector: 'app-admin-submission-review',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule
  ],
  templateUrl: './submission-review.component.html',
  styleUrl: './submission-review.component.scss'
})
export class AdminSubmissionReviewComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly route = inject(ActivatedRoute);

  readonly decisionActions: DecisionAction[] = [
    {
      value: 'APPROVE',
      label: 'Approve',
      icon: 'check_circle',
      description: 'Publish this icon and its church and saint records in the public archive.'
    },
    {
      value: 'REQUEST_CORRECTION',
      label: 'Request correction',
      icon: 'edit_note',
      description: 'Ask the contributor to fix the metadata before the submission can be reviewed again.'
    },
    {
      value: 'REJECT',
      label: 'Reject',
      icon: 'cancel',
      description: 'Decline this submission. The record stays unpublished and its images may be cleaned up.'
    }
  ];

  readonly notes = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(4000)]
  });

  submission: AdminSubmissionDetail | null = null;
  loading = true;
  error = '';
  successMessage = '';

  selectedAction: ReviewAction | null = null;
  submitting = false;
  actionError = '';

  private submissionId = '';

  ngOnInit(): void {
    this.submissionId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.submissionId) {
      this.loading = false;
      this.error = 'This submission could not be found.';
      return;
    }
    this.loadSubmission();
  }

  loadSubmission(): void {
    this.loading = true;
    this.error = '';

    this.adminService.getSubmission(this.submissionId).subscribe({
      next: (submission) => {
        this.submission = submission;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.loading = false;
        this.error = this.errorMessage(error, 'Unable to load this submission.');
      }
    });
  }

  selectAction(action: ReviewAction): void {
    this.selectedAction = action;
    this.actionError = '';
    this.notes.reset('');
    this.notes.markAsPristine();
    this.notes.markAsUntouched();
  }

  cancelDecision(): void {
    this.selectedAction = null;
    this.actionError = '';
    this.notes.reset('');
  }

  submitDecision(): void {
    if (!this.selectedAction || !this.submission || this.submitting) {
      return;
    }

    const notes = this.notes.value.trim();
    if (this.requiresNotes && !notes) {
      this.notes.markAsTouched();
      this.actionError = 'Add a moderation note explaining this decision.';
      return;
    }
    if (this.notes.invalid) {
      this.notes.markAsTouched();
      this.actionError = 'Moderation notes must be 4000 characters or fewer.';
      return;
    }

    const action = this.selectedAction;
    this.submitting = true;
    this.actionError = '';

    this.adminService
      .decide(this.submission.id, { action, notes: notes || undefined })
      .subscribe({
        next: (response) => {
          this.submitting = false;
          this.selectedAction = null;
          this.notes.reset('');
          this.successMessage = response.message || 'The moderation decision was recorded.';
          this.loadSubmission();
        },
        error: (error: unknown) => {
          this.submitting = false;
          this.actionError = this.errorMessage(error, 'Unable to record this decision.');
        }
      });
  }

  get canDecide(): boolean {
    return this.submission?.status === 'PENDING' || this.submission?.status === 'NEEDS_CORRECTION';
  }

  get requiresNotes(): boolean {
    return this.selectedAction !== null && this.selectedAction !== 'APPROVE';
  }

  get selectedActionDetails(): DecisionAction | null {
    return this.decisionActions.find((action) => action.value === this.selectedAction) ?? null;
  }

  get notesLength(): number {
    return this.notes.value.length;
  }

  statusLabel(status: SubmissionStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  statusClass(status: SubmissionStatus): string {
    return `status-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  actionLabel(action: string): string {
    return ACTION_LABELS[action] ?? action.replace(/_/g, ' ').toLowerCase();
  }

  formatValue(value?: string | null): string {
    if (!value) {
      return '—';
    }
    return value.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (character) => character.toUpperCase());
  }

  subjectLabel(subject: IconSubject): string {
    return subject.saint?.canonicalName || subject.subjectLabel || 'Unidentified figure';
  }

  imageAlt(image: IconImage, index: number): string {
    return image.altText?.trim() || `${this.submission?.title ?? 'Submission'} — image ${index + 1}`;
  }

  imageLabel(image: IconImage, index: number): string {
    return image.primary ? 'Primary image' : `Image ${index + 1}`;
  }

  imageDimensions(image: IconImage): string {
    return image.widthPx && image.heightPx ? `${image.widthPx} × ${image.heightPx}px` : '';
  }

  imageFileSize(image: IconImage): string {
    if (!image.fileSizeBytes) {
      return '';
    }
    if (image.fileSizeBytes < 1024 * 1024) {
      return `${Math.round(image.fileSizeBytes / 1024)} KB`;
    }
    return `${(image.fileSizeBytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  churchLocation(): string {
    const church = this.submission?.church;
    if (!church) {
      return '—';
    }
    const parts = [church.town, church.region, church.country].filter(
      (part): part is string => Boolean(part)
    );
    return parts.length > 0 ? parts.join(', ') : '—';
  }

  artworkDateRange(): string {
    const start = this.submission?.artworkDateStart;
    const end = this.submission?.artworkDateEnd;
    if (start && end) {
      return start === end ? start : `${start} – ${end}`;
    }
    return start || end || '—';
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Please sign in to review submissions.';
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
