import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { IconService } from '../../services/icon.service';
import { IconSummary } from '../../models/icon.model';

@Component({
  selector: 'app-icons',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    RouterLink,
  ],
  templateUrl: './icons.component.html',
  styleUrl: './icons.component.scss',
})
export class IconsComponent implements OnInit {
  private readonly iconService = inject(IconService);

  icons: IconSummary[] = [];
  loading = true;
  error = false;

  filterForm = new FormGroup({
    q: new FormControl(''),
    region: new FormControl(''),
    churchId: new FormControl(''),
    saintId: new FormControl(''),
    category: new FormControl(''),
    sort: new FormControl('newest'),
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
  
  categories = [
    'saint',
    'christ',
    'mary',
    'trinity',
    'cross',
    'miracle',
    'feast',
  ];

  ngOnInit(): void {
    this.loadIcons();
    this.filterForm.valueChanges.subscribe(() => {
      this.loadIcons();
    });
  }

  loadIcons(): void {
    this.loading = true;
    this.error = false;
    const values = this.filterForm.value;

    this.iconService
      .getIcons({
        q: values.q || undefined,
        region: values.region || undefined,
        churchId: values.churchId || undefined,
        saintId: values.saintId || undefined,
        category: values.category || undefined,
        sort: values.sort || undefined,
        size: 24,
      })
      .subscribe({
        next: (page) => {
          this.icons = page.content || [];
          this.loading = false;
        },
        error: () => {
          this.error = true;
          this.loading = false;
        },
      });
  }

  clearFilters(): void {
    this.filterForm.reset({
      q: '',
      region: '',
      churchId: '',
      saintId: '',
      category: '',
      sort: 'newest',
    });
  }

  getDisplayName(icon: IconSummary): string {
    if (icon.saintNames && icon.saintNames.length > 0) {
      return icon.saintNames.join(', ');
    }
    return icon.depictedFigure || icon.title || 'Unknown';
  }

  getLocation(icon: IconSummary): string {
    const parts = [icon.town, icon.region, icon.country].filter((p) => !!p);
    return parts.length > 0 ? parts.join(', ') : '—';
  }
}
