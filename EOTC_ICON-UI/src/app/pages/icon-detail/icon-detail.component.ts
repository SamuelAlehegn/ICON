import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { IconDetail, IconImage, IconSubject, SaintSummary } from '../../models/icon.model';
import { IconService } from '../../services/icon.service';

interface VerificationItem {
  label: string;
  value: string;
  tone: 'positive' | 'warning' | 'neutral';
}

@Component({
  selector: 'app-icon-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatProgressBarModule],
  templateUrl: './icon-detail.component.html',
  styleUrl: './icon-detail.component.scss',
})
export class IconDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly iconService = inject(IconService);
  private readonly destroyRef = inject(DestroyRef);

  icon: IconDetail | null = null;
  loading = true;
  error = false;
  notFound = false;
  mainImageFailed = false;

  private currentId = '';
  private requestId = 0;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = params.get('id');
      if (!id) {
        this.requestId += 1;
        this.icon = null;
        this.notFound = true;
        this.loading = false;
        return;
      }

      this.loadIcon(id);
    });
  }

  loadIcon(idOrSlug: string): void {
    const requestId = ++this.requestId;
    this.currentId = idOrSlug;
    this.loading = true;
    this.error = false;
    this.notFound = false;
    this.mainImageFailed = false;

    this.iconService.getIcon(idOrSlug).subscribe({
      next: (icon) => {
        if (requestId !== this.requestId) {
          return;
        }

        this.icon = icon;
        this.loading = false;
      },
      error: (error: unknown) => {
        if (requestId !== this.requestId) {
          return;
        }

        this.icon = null;
        this.loading = false;
        this.notFound = error instanceof HttpErrorResponse && error.status === 404;
        this.error = !this.notFound;
      },
    });
  }

  retry(): void {
    if (this.currentId) {
      this.loadIcon(this.currentId);
    }
  }

  onMainImageError(): void {
    this.mainImageFailed = true;
  }

  get orderedSubjects(): IconSubject[] {
    return [...(this.icon?.subjects ?? [])].sort((a, b) => {
      if (a.primary !== b.primary) {
        return a.primary ? -1 : 1;
      }
      return a.sortOrder - b.sortOrder;
    });
  }

  get primarySubject(): IconSubject | null {
    return this.orderedSubjects[0] ?? null;
  }

  get primarySaint(): SaintSummary | null {
    const withSaint = this.orderedSubjects.find((subject) => subject.saint);
    return withSaint?.saint ?? null;
  }

  get saintName(): string {
    return (
      this.primarySubject?.saint?.canonicalName?.trim() ||
      this.primarySubject?.subjectLabel?.trim() ||
      this.primarySaint?.canonicalName?.trim() ||
      'Figure not identified'
    );
  }

  get alternativeNames(): string[] {
    const excluded = new Set(
      [this.saintName, this.primarySaint?.canonicalName ?? '']
        .map((name) => name.trim().toLocaleLowerCase())
        .filter(Boolean)
    );
    const names: string[] = [];

    for (const subject of this.orderedSubjects) {
      for (const name of [subject.subjectLabel, subject.saint?.canonicalName]) {
        const trimmed = name?.trim();
        if (!trimmed) {
          continue;
        }

        const key = trimmed.toLocaleLowerCase();
        if (excluded.has(key) || names.some((existing) => existing.toLocaleLowerCase() === key)) {
          continue;
        }

        names.push(trimmed);
      }
    }

    return names;
  }

  get amharicName(): string | null {
    return this.primarySaint?.amharicName?.trim() || null;
  }

  get geezName(): string | null {
    return this.primarySaint?.geezName?.trim() || null;
  }

  get mainImage(): IconImage | null {
    const images = this.icon?.images ?? [];
    return images.find((image) => image.primary) ?? images[0] ?? null;
  }

  get mainImageUrl(): string | null {
    if (this.mainImageFailed) {
      return null;
    }
    return this.mainImage?.imageUrl || this.mainImage?.thumbnailUrl || null;
  }

  get mainImageAlt(): string {
    return this.mainImage?.altText?.trim() || `Icon depicting ${this.saintName}`;
  }

  get churchName(): string {
    return this.icon?.church?.name?.trim() || 'Not recorded';
  }

  get town(): string {
    return this.icon?.church?.town?.trim() || 'Not recorded';
  }

  get region(): string {
    return this.icon?.church?.region?.trim() || 'Not recorded';
  }

  get country(): string {
    return this.icon?.church?.country?.trim() || 'Not recorded';
  }

  get contributorCredit(): string {
    return this.icon?.contributorCredit?.trim() || 'Anonymous contributor';
  }

  get captureDateLabel(): string | null {
    if (this.icon?.captureDate) {
      return this.formatDate(this.icon.captureDate, true);
    }
    return this.formatDate(this.mainImage?.capturedAt);
  }

  get submittedDateLabel(): string | null {
    return this.formatDate(this.icon?.createdAt);
  }

  get publishedDateLabel(): string | null {
    return this.formatDate(this.icon?.publishedAt);
  }

  get verificationItems(): VerificationItem[] {
    const items: VerificationItem[] = [
      {
        label: 'Publication',
        value: this.icon?.publishedAt ? 'Approved and published' : 'Approved',
        tone: 'positive',
      },
    ];

    const certainty = this.icon?.figureCertainty || this.primarySubject?.figureCertainty;
    items.push({
      label: 'Figure identification',
      value: this.certaintyLabel(certainty),
      tone: this.certaintyTone(certainty),
    });

    const saint = this.primarySaint;
    items.push({
      label: 'Saint record',
      value: saint ? (saint.verified ? 'Verified' : 'Not yet verified') : 'No saint record linked',
      tone: saint ? (saint.verified ? 'positive' : 'warning') : 'neutral',
    });

    const rights = this.mainImage?.rights;
    items.push({
      label: 'Image rights',
      value: rights ? (rights.verified ? 'Verified' : 'Not yet verified') : 'Not recorded',
      tone: rights ? (rights.verified ? 'positive' : 'warning') : 'neutral',
    });

    return items;
  }

  get verificationSummaryLabel(): string {
    return this.icon?.publishedAt ? 'Approved and published' : 'Approved';
  }

  private certaintyLabel(value: string | null | undefined): string {
    switch ((value ?? '').toUpperCase()) {
      case 'IDENTIFIED':
        return 'Identified';
      case 'PROBABLE':
        return 'Probable';
      case 'UNKNOWN':
        return 'Not identified';
      default:
        return 'Not recorded';
    }
  }

  private certaintyTone(value: string | null | undefined): VerificationItem['tone'] {
    switch ((value ?? '').toUpperCase()) {
      case 'IDENTIFIED':
        return 'positive';
      case 'PROBABLE':
        return 'warning';
      default:
        return 'neutral';
    }
  }

  private formatDate(value: string | null | undefined, dateOnly = false): string | null {
    if (!value) {
      return null;
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }

    return new Intl.DateTimeFormat('en', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      timeZone: dateOnly ? 'UTC' : undefined,
    }).format(date);
  }
}
