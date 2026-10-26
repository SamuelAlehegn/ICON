import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ArchiveStats } from '../models/archive-stats.model';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getStatistics(): Observable<ArchiveStats> {
    return this.http.get<ArchiveStats>(`${this.baseUrl}/api/v1/statistics`);
  }
}
