import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ContributionCreatedResponse {
  referenceCode: string;
  status: string;
  message: string;
}

export interface ContributionStatusResponse {
  referenceCode: string;
  status: string;
  title: string;
  churchName: string;
  depictedFigure: string;
  imageCount: number;
  submittedAt: string;
  updatedAt: string;
  moderatorNotes?: string;
}

export interface UploadProgress {
  progress: number;
  response?: ContributionCreatedResponse;
}

@Injectable({
  providedIn: 'root'
})
export class ContributionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  submitContribution(formData: FormData): Observable<UploadProgress> {
    const req = new HttpRequest('POST', `${this.baseUrl}/api/v1/contributions`, formData, {
      reportProgress: true
    });

    return this.http.request<ContributionCreatedResponse>(req).pipe(
      map((event: HttpEvent<ContributionCreatedResponse>): UploadProgress => {
        if (event.type === HttpEventType.UploadProgress) {
          const total = event.total ?? 0;
          const progress = total > 0 ? Math.round((100 * event.loaded) / total) : 0;
          return { progress };
        } else if (event.type === HttpEventType.Response) {
          return { progress: 100, response: event.body ?? undefined };
        }
        return { progress: 0 };
      })
    );
  }

  getStatus(referenceCode: string): Observable<ContributionStatusResponse> {
    return this.http.get<ContributionStatusResponse>(`${this.baseUrl}/api/v1/contributions/status/${encodeURIComponent(referenceCode)}`);
  }
}
