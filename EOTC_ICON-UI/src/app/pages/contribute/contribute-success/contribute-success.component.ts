import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { ContributionService, ContributionStatusResponse } from '../../../services/contribution.service';

@Component({
  selector: 'app-contribute-success',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, MatCardModule],
  templateUrl: './contribute-success.component.html',
  styleUrl: './contribute-success.component.scss'
})
export class ContributeSuccessComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly contributionService = inject(ContributionService);

  referenceCode = '';
  status: ContributionStatusResponse | null = null;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.referenceCode = this.route.snapshot.queryParamMap.get('ref') || '';
    if (!this.referenceCode) {
      this.loading = false;
      this.error = 'No submission reference provided.';
      return;
    }

    this.contributionService.getStatus(this.referenceCode).subscribe({
      next: (res) => {
        this.status = res;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load status', err);
        this.error = 'Unable to load submission status.';
        this.loading = false;
      }
    });
  }

  goHome(): void {
    this.router.navigate(['/']);
  }

  contributeAgain(): void {
    this.router.navigate(['/contribute']);
  }
}
