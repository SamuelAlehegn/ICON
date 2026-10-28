import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import {
  AccountSubmission,
  AttributionPreference,
  SubmissionStatus,
  UpdateSubmissionPayload
} from '../../models/submission.model';
import { AccountService } from '../../services/account.service';

type RequestType = 'correction' | 'removal';

interface RequestTarget {
  id: string;
  type: RequestType;
}

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

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule
  ],
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss'
})
export class AccountComponent implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly fb = inject(FormBuilder);

  submissions: AccountSubmission[] = [];
  loading = true;
  error = '';
  successMessage = '';

  editingId: string | null = null;
  editForm!: FormGroup;
  saving = false;

  requestTarget: RequestTarget | null = null;
  readonly requestNotes = new FormControl('', { nonNullable: true, validators: [Validators.maxLength(4000)] });
  requesting = false;

  readonly attributionOptions: { value: AttributionPreference; label: string }[] = [
    { value: 'DISPLAY_NAME', label: 'Display my name' },
    { value: 'ANONYMOUS', label: 'Stay anonymous' }
  ];

  ngOnInit(): void {
    this.editForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(240)]],
      description: ['', [Validators.maxLength(4000)]],
      historicalNotes: ['', [Validators.maxLength(4000)]],
      category: ['', [Validators.maxLength(80)]],
      captureDate: [''],
      artworkDateStart: [''],
      artworkDateEnd: [''],
      attributionPreference: ['DISPLAY_NAME' as AttributionPreference, [Validators.required]],
      contributorDisplayName: ['', [Validators.maxLength(120)]],
      contactEmail: ['', [Validators.email, Validators.maxLength(320)]]
    });
    this.loadSubmissions();
  }

  loadSubmissions(): void {
    this.loading = true;
    this.error = '';
    this.accountService.getSubmissions().subscribe({
      next: (submissions) => {
        this.submissions = submissions;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.loading = false;
        this.error = this.errorMessage(error, 'Unable to load your submissions.');
      }
    });
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

  isEditable(submission: AccountSubmission): boolean {
    return submission.status === 'PENDING' || submission.status === 'NEEDS_CORRECTION';
  }

  canRequestCorrection(submission: AccountSubmission): boolean {
    return submission.status !== 'ARCHIVED';
  }

  canRequestRemoval(submission: AccountSubmission): boolean {
    return submission.status !== 'ARCHIVED';
  }

  primaryImageUrl(submission: AccountSubmission): string | null {
    const images = submission.images ?? [];
    const image = images.find((item) => item.primary) ?? images[0];
    return image?.thumbnailUrl || image?.imageUrl || null;
  }

  startEdit(submission: AccountSubmission): void {
    this.editingId = submission.id;
    this.requestTarget = null;
    this.successMessage = '';
    this.editForm.reset({
      title: submission.title,
      description: submission.description ?? '',
      historicalNotes: submission.historicalNotes ?? '',
      category: submission.category ?? '',
      captureDate: submission.captureDate ?? '',
      artworkDateStart: submission.artworkDateStart ?? '',
      artworkDateEnd: submission.artworkDateEnd ?? '',
      attributionPreference: submission.attributionPreference,
      contributorDisplayName: submission.contributorDisplayName ?? '',
      contactEmail: submission.contactEmail ?? ''
    });
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(submission: AccountSubmission): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const value = this.editForm.getRawValue();
    const payload: UpdateSubmissionPayload = {
      title: value.title,
      description: value.description,
      historicalNotes: value.historicalNotes,
      category: value.category,
      captureDate: value.captureDate || null,
      artworkDateStart: value.artworkDateStart || null,
      artworkDateEnd: value.artworkDateEnd || null,
      attributionPreference: value.attributionPreference,
      contributorDisplayName: value.contributorDisplayName,
      contactEmail: value.contactEmail
    };

    this.saving = true;
    this.error = '';
    this.accountService.updateSubmission(submission.id, payload).subscribe({
      next: (updated) => {
        this.replaceSubmission(updated);
        this.editingId = null;
        this.saving = false;
        this.successMessage = 'Your submission was updated.';
      },
      error: (error: unknown) => {
        this.saving = false;
        this.error = this.errorMessage(error, 'Unable to update this submission.');
      }
    });
  }

  startRequest(submission: AccountSubmission, type: RequestType): void {
    this.requestTarget = { id: submission.id, type };
    this.editingId = null;
    this.successMessage = '';
    this.requestNotes.reset('');
  }

  cancelRequest(): void {
    this.requestTarget = null;
    this.requestNotes.reset('');
  }

  submitRequest(): void {
    if (!this.requestTarget) {
      return;
    }

    const target = this.requestTarget;
    const notes = this.requestNotes.value.trim();
    this.requesting = true;
    this.error = '';

    const request$ = target.type === 'correction'
      ? this.accountService.requestCorrection(target.id, notes)
      : this.accountService.requestRemoval(target.id, notes);

    request$.subscribe({
      next: (response) => {
        this.requesting = false;
        this.requestTarget = null;
        this.requestNotes.reset('');
        this.successMessage = response.message;
        this.loadSubmissions();
      },
      error: (error: unknown) => {
        this.requesting = false;
        this.error = this.errorMessage(error, 'Unable to send your request.');
      }
    });
  }

  private replaceSubmission(updated: AccountSubmission): void {
    this.submissions = this.submissions.map((submission) =>
      submission.id === updated.id ? updated : submission
    );
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Please sign in to manage your submissions.';
      }
      if (error.status === 403) {
        return 'You do not have permission to manage this submission.';
      }
      const message = (error.error as { message?: unknown } | null)?.message;
      if (typeof message === 'string' && message.trim()) {
        return message;
      }
    }
    return fallback;
  }
}
