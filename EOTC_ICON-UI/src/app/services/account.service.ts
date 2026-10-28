import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AccountSubmission,
  SubmissionRequestResponse,
  UpdateSubmissionPayload
} from '../models/submission.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getSubmissions(): Observable<AccountSubmission[]> {
    return this.http.get<AccountSubmission[]>(`${this.baseUrl}/api/v1/account/submissions`);
  }

  getSubmission(id: string): Observable<AccountSubmission> {
    return this.http.get<AccountSubmission>(
      `${this.baseUrl}/api/v1/account/submissions/${encodeURIComponent(id)}`
    );
  }

  updateSubmission(id: string, payload: UpdateSubmissionPayload): Observable<AccountSubmission> {
    return this.http.patch<AccountSubmission>(
      `${this.baseUrl}/api/v1/account/submissions/${encodeURIComponent(id)}`,
      payload
    );
  }

  requestCorrection(id: string, notes: string): Observable<SubmissionRequestResponse> {
    return this.http.post<SubmissionRequestResponse>(
      `${this.baseUrl}/api/v1/account/submissions/${encodeURIComponent(id)}/request-correction`,
      { notes }
    );
  }

  requestRemoval(id: string, notes: string): Observable<SubmissionRequestResponse> {
    return this.http.post<SubmissionRequestResponse>(
      `${this.baseUrl}/api/v1/account/submissions/${encodeURIComponent(id)}/request-removal`,
      { notes }
    );
  }
}
