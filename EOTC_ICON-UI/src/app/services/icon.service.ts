import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { IconDetail, IconSummary } from '../models/icon.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({
  providedIn: 'root'
})
export class IconService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getIcons(params: {
    q?: string;
    region?: string;
    churchId?: string;
    saintId?: string;
    category?: string;
    sort?: string;
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<IconSummary>> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.region) httpParams = httpParams.set('region', params.region);
    if (params.churchId) httpParams = httpParams.set('churchId', params.churchId);
    if (params.saintId) httpParams = httpParams.set('saintId', params.saintId);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());

    return this.http.get<PageResponse<IconSummary>>(`${this.baseUrl}/api/v1/icons`, { params: httpParams });
  }

  getIcon(idOrSlug: string): Observable<IconDetail> {
    return this.http.get<IconDetail>(`${this.baseUrl}/api/v1/icons/${encodeURIComponent(idOrSlug)}`);
  }
}
