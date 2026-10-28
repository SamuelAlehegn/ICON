import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChurchDetail, ChurchSummary } from '../models/church.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({
  providedIn: 'root'
})
export class ChurchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getChurches(params: { page?: number; size?: number } = {}): Observable<PageResponse<ChurchSummary>> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<PageResponse<ChurchSummary>>(`${this.baseUrl}/api/v1/churches`, { params: httpParams });
  }

  getChurch(idOrSlug: string): Observable<ChurchDetail> {
    return this.http.get<ChurchDetail>(`${this.baseUrl}/api/v1/churches/${encodeURIComponent(idOrSlug)}`);
  }
}
