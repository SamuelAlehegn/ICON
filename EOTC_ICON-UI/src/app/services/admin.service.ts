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
}
