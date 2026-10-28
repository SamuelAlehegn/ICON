import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ChurchService } from '../../../services/church.service';
import { ChurchDetail } from '../../../models/church.model';

@Component({
  selector: 'app-church-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule, MatProgressBarModule],
  templateUrl: './church-detail.component.html',
  styleUrl: './church-detail.component.scss'
})
export class ChurchDetailComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly churchService = inject(ChurchService);

  church: ChurchDetail | null = null;
  loading = true;
  error = false;

  ngOnInit(): void {
    const id = this.activatedRoute.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/churches']);
      return;
    }
    this.loadChurch(id);
  }

  loadChurch(id: string): void {
    this.loading = true;
    this.error = false;
    this.churchService.getChurch(id).subscribe({
      next: (res) => {
        this.church = res;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  retry(): void {
    const id = this.activatedRoute.snapshot.paramMap.get('id');
    if (id) {
      this.loadChurch(id);
    } else {
      this.goBack();
    }
  }

  goBack(): void {
    this.router.navigate(['/churches']);
  }

  getLocation(church: ChurchDetail): string {
    const parts = [church.town, church.region, church.country].filter(Boolean);
    return parts.length > 0 ? parts.join(', ') : '—';
  }
}
