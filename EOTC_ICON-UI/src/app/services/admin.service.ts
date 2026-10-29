import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AdminSubmissionDetail,
  AdminSubmissionSummary,
  ReviewDecisionPayload,
  ReviewDecisionResponse
} from '../models/admin.model';
import { PageResponse } from '../models/page-response.model';
import { SubmissionStatus } from '../models/submission.model';
import { ChurchSummary } from '../models/church.model';
import { SaintSummary } from '../models/saint.model';
import { AdminAuditEntry, AdminRoleUpdate, AdminUser, ArchiveStatistics, ChurchAdminPayload, SaintAdminPayload } from '../models/admin-management.model';

export interface AdminQueueParams {
  status?: SubmissionStatus;
  q?: string;
  page?: number;
  size?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getQueue(params: AdminQueueParams = {}): Observable<PageResponse<AdminSubmissionSummary>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 12));

    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.q?.trim()) {
      httpParams = httpParams.set('q', params.q.trim());
    }

    return this.http.get<PageResponse<AdminSubmissionSummary>>(
      `${this.baseUrl}/api/v1/admin/submissions`,
      { params: httpParams }
    );
  }

  getSubmission(id: string): Observable<AdminSubmissionDetail> {
    return this.http.get<AdminSubmissionDetail>(
      `${this.baseUrl}/api/v1/admin/submissions/${encodeURIComponent(id)}`
    );
  }

  decide(id: string, payload: ReviewDecisionPayload): Observable<ReviewDecisionResponse> {
    return this.http.post<ReviewDecisionResponse>(
      `${this.baseUrl}/api/v1/admin/submissions/${encodeURIComponent(id)}/decision`,
      payload
    );
  }

  getStatistics(): Observable<ArchiveStatistics> {
    return this.http.get<ArchiveStatistics>(`${this.baseUrl}/api/v1/admin/statistics`);
  }

  getUsers(page = 0, size = 50): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(`${this.baseUrl}/api/v1/admin/users`, {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  setUserRole(id: string, payload: AdminRoleUpdate): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.baseUrl}/api/v1/admin/users/${encodeURIComponent(id)}/role`, payload);
  }

  getAuditLog(page = 0, size = 50): Observable<PageResponse<AdminAuditEntry>> {
    return this.http.get<PageResponse<AdminAuditEntry>>(`${this.baseUrl}/api/v1/admin/audit-log`, {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  createChurch(payload: ChurchAdminPayload): Observable<ChurchSummary> {
    return this.http.post<ChurchSummary>(`${this.baseUrl}/api/v1/admin/churches`, payload);
  }

  updateChurch(id: string, payload: ChurchAdminPayload): Observable<ChurchSummary> {
    return this.http.put<ChurchSummary>(`${this.baseUrl}/api/v1/admin/churches/${encodeURIComponent(id)}`, payload);
  }

  mergeChurch(id: string, targetId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/v1/admin/churches/${encodeURIComponent(id)}/merge`, { targetId });
  }

  createSaint(payload: SaintAdminPayload): Observable<SaintSummary> {
    return this.http.post<SaintSummary>(`${this.baseUrl}/api/v1/admin/saints`, payload);
  }

  updateSaint(id: string, payload: SaintAdminPayload): Observable<SaintSummary> {
    return this.http.put<SaintSummary>(`${this.baseUrl}/api/v1/admin/saints/${encodeURIComponent(id)}`, payload);
  }

  mergeSaint(id: string, targetId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/v1/admin/saints/${encodeURIComponent(id)}/merge`, { targetId });
  }
}
