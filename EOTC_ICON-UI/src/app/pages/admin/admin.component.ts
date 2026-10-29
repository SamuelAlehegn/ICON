import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
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
import { ChurchService } from '../../services/church.service';
import { SaintService } from '../../services/saint.service';
import { ChurchDetail, ChurchSummary } from '../../models/church.model';
import { SaintDetail, SaintSummary } from '../../models/saint.model';
import { AdminAuditEntry, AdminUser, ArchiveStatistics, ChurchAdminPayload, SaintAdminPayload } from '../../models/admin-management.model';
import { UserRole } from '../../models/auth.model';

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
    FormsModule,
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
  private readonly churchService = inject(ChurchService);
  private readonly saintService = inject(SaintService);
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
  section: 'queue' | 'churches' | 'saints' | 'users' | 'statistics' | 'audit' = 'queue';
  churches: ChurchSummary[] = [];
  saints: SaintSummary[] = [];
  users: AdminUser[] = [];
  auditEntries: AdminAuditEntry[] = [];
  statistics: ArchiveStatistics | null = null;
  roles: UserRole[] = ['CONTRIBUTOR', 'MODERATOR', 'ADMIN'];
  editingChurchId: string | null = null;
  editingSaintId: string | null = null;
  churchDraft: ChurchAdminPayload = this.emptyChurch();
  saintDraft: SaintAdminPayload = this.emptySaint();
  churchMergeSource = '';
  churchMergeTarget = '';
  saintMergeSource = '';
  saintMergeTarget = '';
  managementLoading = false;
  managementError = '';
  managementNotice = '';
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
    this.loadManagementData();
  }

  selectSection(section: typeof this.section): void {
    this.section = section;
    this.managementNotice = '';
    this.managementError = '';
    this.loadManagementData();
  }

  loadManagementData(): void {
    switch (this.section) {
      case 'churches':
        this.churchService.getChurches({ page: 0, size: 1000 }).subscribe({ next: r => this.churches = r.content ?? [], error: e => this.setManagementError(e) });
        break;
      case 'saints':
        this.saintService.getSaints({ page: 0, size: 1000 }).subscribe({ next: r => this.saints = r.content ?? [], error: e => this.setManagementError(e) });
        break;
      case 'users':
        this.adminService.getUsers().subscribe({ next: r => this.users = r.content ?? [], error: e => this.setManagementError(e) });
        break;
      case 'statistics':
        this.adminService.getStatistics().subscribe({ next: r => this.statistics = r, error: e => this.setManagementError(e) });
        break;
      case 'audit':
        this.adminService.getAuditLog().subscribe({ next: r => this.auditEntries = r.content ?? [], error: e => this.setManagementError(e) });
        break;
    }
  }

  saveChurch(): void {
    this.managementLoading = true;
    const request = this.editingChurchId
      ? this.adminService.updateChurch(this.editingChurchId, this.churchDraft)
      : this.adminService.createChurch(this.churchDraft);
    request.subscribe({ next: () => { this.managementNotice = this.editingChurchId ? 'Church updated.' : 'Church created.'; this.resetChurch(); this.managementLoading = false; this.loadManagementData(); }, error: e => { this.setManagementError(e); this.managementLoading = false; } });
  }

  editChurch(church: ChurchSummary): void {
    this.churchService.getChurch(church.id).subscribe({
      next: (detail: ChurchDetail) => {
        this.editingChurchId = detail.id;
        this.churchDraft = { name: detail.name, region: detail.region, town: detail.town, country: detail.country,
          latitude: detail.latitude, longitude: detail.longitude, description: detail.description };
      },
      error: error => this.setManagementError(error)
    });
  }

  mergeChurch(): void {
    if (!this.churchMergeSource || !this.churchMergeTarget || this.churchMergeSource === this.churchMergeTarget) return;
    this.managementLoading = true;
    this.adminService.mergeChurch(this.churchMergeSource, this.churchMergeTarget).subscribe({ next: () => { this.managementNotice = 'Duplicate church merged.'; this.churchMergeSource = ''; this.churchMergeTarget = ''; this.managementLoading = false; this.loadManagementData(); }, error: e => { this.setManagementError(e); this.managementLoading = false; } });
  }

  saveSaint(): void {
    this.managementLoading = true;
    const request = this.editingSaintId
      ? this.adminService.updateSaint(this.editingSaintId, this.saintDraft)
      : this.adminService.createSaint(this.saintDraft);
    request.subscribe({ next: () => { this.managementNotice = this.editingSaintId ? 'Saint updated.' : 'Saint created.'; this.resetSaint(); this.managementLoading = false; this.loadManagementData(); }, error: e => { this.setManagementError(e); this.managementLoading = false; } });
  }

  editSaint(saint: SaintSummary): void {
    this.saintService.getSaint(saint.id).subscribe({
      next: (detail: SaintDetail) => {
        this.editingSaintId = detail.id;
        this.saintDraft = { canonicalName: detail.canonicalName, amharicName: detail.amharicName,
          geezName: detail.geezName, category: detail.category, verified: detail.verified, description: detail.description };
      },
      error: error => this.setManagementError(error)
    });
  }

  mergeSaint(): void {
    if (!this.saintMergeSource || !this.saintMergeTarget || this.saintMergeSource === this.saintMergeTarget) return;
    this.managementLoading = true;
    this.adminService.mergeSaint(this.saintMergeSource, this.saintMergeTarget).subscribe({ next: () => { this.managementNotice = 'Duplicate saint merged.'; this.saintMergeSource = ''; this.saintMergeTarget = ''; this.managementLoading = false; this.loadManagementData(); }, error: e => { this.setManagementError(e); this.managementLoading = false; } });
  }

  changeRole(user: AdminUser, role: UserRole): void {
    this.adminService.setUserRole(user.id, { role }).subscribe({ next: updated => { this.users = this.users.map(item => item.id === updated.id ? updated : item); this.managementNotice = `Role updated for ${updated.displayName}.`; }, error: e => this.setManagementError(e) });
  }

  churchLocation(church: ChurchSummary): string {
    return [church.town, church.region, church.country].filter((part): part is string => Boolean(part)).join(', ');
  }

  resetChurch(): void { this.editingChurchId = null; this.churchDraft = this.emptyChurch(); }
  resetSaint(): void { this.editingSaintId = null; this.saintDraft = this.emptySaint(); }
  private emptyChurch(): ChurchAdminPayload { return { name: '', country: 'Ethiopia' }; }
  private emptySaint(): SaintAdminPayload { return { canonicalName: '', verified: false }; }
  private setManagementError(error: unknown): void {
    const apiMessage = (error as { error?: { message?: unknown } })?.error?.message;
    this.managementError = typeof apiMessage === 'string' ? apiMessage : 'Unable to load or save this admin data.';
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
