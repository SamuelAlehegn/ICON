import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PageResponse, SaintDetail, SaintSummary } from '../models/icon.model';

@Injectable({
  providedIn: 'root'
})
export class SaintService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getSaints(params: {
    q?: string;
    category?: string;
    sort?: string;
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<SaintSummary>> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());

    return this.http.get<PageResponse<SaintSummary>>(`${this.baseUrl}/api/v1/saints`, { params: httpParams });
  }

  getSaint(idOrSlug: string): Observable<SaintDetail> {
    return this.http.get<SaintDetail>(`${this.baseUrl}/api/v1/saints/${encodeURIComponent(idOrSlug)}`);
  }
}
