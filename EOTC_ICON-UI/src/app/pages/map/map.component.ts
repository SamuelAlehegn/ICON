import { Component, AfterViewInit, ElementRef, OnDestroy, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import * as L from 'leaflet';
import { ChurchService } from '../../services/church.service';
import { ChurchSummary } from '../../models/church.model';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss'
})
export class MapComponent implements AfterViewInit, OnDestroy {
  private readonly churchService = inject(ChurchService);

  @ViewChild('mapContainer') private mapContainer?: ElementRef<HTMLDivElement>;
  private map?: L.Map;
  private markersLayer?: L.LayerGroup;

  churchesWithCoords: ChurchSummary[] = [];
  churchesWithoutCoords: ChurchSummary[] = [];
  allChurches: ChurchSummary[] = [];
  loading = true;
  error = false;

  ngAfterViewInit(): void {
    this.initMap();
    this.loadChurchMarkers();
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  private initMap(): void {
    if (!this.mapContainer?.nativeElement) {
      return;
    }
    this.map = L.map(this.mapContainer.nativeElement).setView([9.145, 38.766], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);
    this.markersLayer = L.layerGroup().addTo(this.map);
  }

  private loadChurchMarkers(): void {
    this.loading = true;
    this.error = false;
    // Try to load all churches - use a reasonably large page size
    this.churchService.getChurches({ page: 0, size: 1000 }).pipe(
      catchError(() => {
        this.error = true;
        this.loading = false;
        return of({ content: [] as ChurchSummary[] } as any);
      })
    ).subscribe((result: any) => {
      const churches: ChurchSummary[] = result?.content || [];
      this.allChurches = churches;
      this.churchesWithCoords = churches.filter(c => c.latitude != null && c.longitude != null);
      this.churchesWithoutCoords = churches.filter(c => c.latitude == null || c.longitude == null);
      this.renderMarkers(churches);
      this.loading = false;
    });
  }

  private renderMarkers(churches: ChurchSummary[]): void {
    if (!this.markersLayer) {
      return;
    }
    this.markersLayer.clearLayers();
    for (const church of churches) {
      if (church.latitude == null || church.longitude == null) {
        continue;
      }
      const lat = Number(church.latitude);
      const lng = Number(church.longitude);
      if (Number.isNaN(lat) || Number.isNaN(lng)) {
        continue;
      }
      const marker = L.marker([lat, lng]);
      const location = this.getLocation(church);
      const popupHtml = `
        <div style="min-width: 160px;">
          <strong>${this.escapeHtml(church.name)}</strong><br/>
          <span>${this.escapeHtml(location)}</span><br/>
          <a href="/churches/${encodeURIComponent(church.id)}">View church page</a>
        </div>
      `;
      marker.bindPopup(popupHtml);
      marker.addTo(this.markersLayer);
    }
  }

  getLocation(church: ChurchSummary): string {
    const parts = [church.town, church.region, church.country].filter(Boolean);
    return parts.length > 0 ? parts.join(', ') : '—';
  }

  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text || '';
    return div.innerHTML;
  }
}
