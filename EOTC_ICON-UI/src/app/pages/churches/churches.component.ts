import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ChurchService } from '../../services/church.service';
import { ChurchSummary } from '../../models/icon.model';

@Component({
  selector: 'app-churches',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule
  ],
  templateUrl: './churches.component.html',
  styleUrl: './churches.component.scss'
})
export class ChurchesComponent implements OnInit {
  private readonly churchService = inject(ChurchService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('resultsHeading') private resultsHeading?: ElementRef<HTMLHeadingElement>;

  churches: ChurchSummary[] = [];
  loading = true;
  error = false;
  currentPage = 0;
  totalElements = 0;
  totalPages = 0;
  pageSize = 12;

  filterForm = new FormGroup({
    q: new FormControl(''),
    region: new FormControl('')
  });

  regions = [
    'Tigray',
    'Afar',
    'Amhara',
    'Oromia',
    'Gambela',
    'Somali',
    'Benishangul-Gumuz',
    'Harari',
    'Sidama',
    'Central Ethiopia Regional State',
    'South West Ethiopia Peoples',
    'Southern Ethiopia Peoples',
    'Addis Ababa',
    'Dire Dawa',
  ];

  private requestId = 0;

  constructor() {
    this.filterForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadChurches(0));
  }

  ngOnInit(): void {
    this.loadChurches(0);
  }

  loadChurches(page = this.currentPage): void {
    const requestId = ++this.requestId;
    this.loading = true;
    this.error = false;

    this.churchService.getChurches({ page, size: this.pageSize }).subscribe({
      next: (result) => {
        if (requestId !== this.requestId) return;
        let content = result.content || [];
        const values = this.filterForm.getRawValue();
        const q = values.q?.toLowerCase().trim();
        const region = values.region;
        if (q) {
          content = content.filter(c => 
            c.name?.toLowerCase().includes(q) || 
            c.town?.toLowerCase().includes(q) ||
            c.region?.toLowerCase().includes(q)
          );
        }
        if (region) {
          content = content.filter(c => c.region === region);
        }
        this.churches = content;
        this.currentPage = result.page ?? page;
        this.totalElements = result.totalElements;
        this.totalPages = result.totalPages;
        this.loading = false;
        setTimeout(() => this.resultsHeading?.nativeElement.focus());
      },
      error: () => {
        if (requestId !== this.requestId) return;
        this.error = true;
        this.loading = false;
      }
    });
  }

  clearFilters(): void {
    this.filterForm.reset({ q: '', region: '' });
  }

  retry(): void {
    this.loadChurches(this.currentPage);
  }

  get hasActiveFilters(): boolean {
    const values = this.filterForm.getRawValue();
    return Boolean(values.q?.trim() || values.region);
  }

  getLocation(church: ChurchSummary): string {
    const parts = [church.town, church.region, church.country].filter(Boolean);
    return parts.length > 0 ? parts.join(', ') : '—';
  }
}
