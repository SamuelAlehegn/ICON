import { Component, inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { StatisticsService } from '../../services/statistics.service';
import { ArchiveStats } from '../../models/archive-stats.model';

interface ArchiveFeature {
  title: string;
  description: string;
}

interface ExplorePath {
  kicker: string;
  label: string;
  description: string;
  path: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private readonly statsService = inject(StatisticsService);

  stats: ArchiveStats | null = null;
  loading = true;

  readonly features: ArchiveFeature[] = [
    {
      title: 'Documented with care',
      description:
        'Each record is built around reliable metadata: origin, setting, material, date, and the people who cared for the work.'
    },
    {
      title: 'Attributed with respect',
      description:
        'Figures, churches, and communities are named only when the evidence allows. Unknown details remain honestly unlabeled.'
    },
    {
      title: 'Reviewed before publication',
      description:
        'Contributions are held privately and reviewed by moderators so that images and their rights are handled responsibly.'
    }
  ];

  readonly explorePaths: ExplorePath[] = [
    {
      kicker: 'The collection',
      label: 'Icons',
      description: 'Browse painted icons and religious wall paintings.',
      path: '/icons'
    },
    {
      kicker: 'Places of worship',
      label: 'Churches',
      description: 'Discover the churches and communities that keep these works.',
      path: '/churches'
    },
    {
      kicker: 'Figures of faith',
      label: 'Saints',
      description: 'Explore the saints and figures depicted across the archive.',
      path: '/saints'
    },
    {
      kicker: 'Across Ethiopia',
      label: 'Map',
      description: 'See where documented churches and their collections are located.',
      path: '/map'
    }
  ];

  ngOnInit(): void {
    this.statsService.getStatistics().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
