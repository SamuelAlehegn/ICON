import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { ContributionService } from '../../services/contribution.service';

interface AttributionOption {
  value: 'display-name' | 'anonymous';
  label: string;
  description: string;
}

interface PhotoPreview {
  file: File;
  url: string;
}

const MAX_PHOTO_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/heic', 'image/heif', 'image/webp'];

@Component({
  selector: 'app-contribute',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatRadioModule,
    MatSelectModule,
  ],
  templateUrl: './contribute.component.html',
  styleUrl: './contribute.component.scss',
})
export class ContributeComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly contributionService = inject(ContributionService);
  private readonly router = inject(Router);

  form!: FormGroup;
  submitted = false;
  photos: PhotoPreview[] = [];
  isDragOver = false;
  photoErrors: string[] = [];

  uploading = false;
  uploadProgress = 0;
  submitError = '';

  readonly attributionOptions: AttributionOption[] = [
    { value: 'display-name', label: 'Display name', description: 'Credit will show as provided below.' },
    { value: 'anonymous', label: 'Anonymous', description: 'No name will be displayed with this contribution.' },
  ];

  readonly categories = [
    'Icon',
    'Mural/Fresco',
    'Triptych',
    'Diptych',
    'Processional Cross/Other',
    'Unknown',
  ];

  ngOnInit(): void {
    this.form = this.fb.group({
      photos: this.fb.array([], [Validators.minLength(1)]),
      churchName: ['', [Validators.required]],
      locationInfo: ['', [Validators.required, Validators.maxLength(500)]],
      town: [''],
      region: [''],
      country: ['Ethiopia'],
      gpsLat: [''],
      gpsLng: [''],
      saintName: [''],
      alternativeNames: this.fb.array([this.createAlternativeNameControl()]),
      description: ['', [Validators.maxLength(2000)]],
      category: [''],
      dates: [''],
      artworkDateStart: [''],
      artworkDateEnd: [''],
      notes: ['', [Validators.maxLength(2000)]],
      contactEmail: ['', [Validators.email, Validators.maxLength(254)]],
      attributionPreference: ['display-name', [Validators.required]],
      attributionName: [''],
      permissionConfirmation: [false, [Validators.requiredTrue]],
    });

    this.form.get('attributionPreference')?.valueChanges.subscribe((value) => {
      const nameControl = this.form.get('attributionName');
      if (value === 'display-name') {
        nameControl?.setValidators([Validators.required, Validators.maxLength(120)]);
      } else {
        nameControl?.clearValidators();
      }
      nameControl?.updateValueAndValidity();
      this.clearSubmitError();
    });

    // Clear validation errors on value changes
    this.form.valueChanges.subscribe(() => {
      if (this.submitted) {
        this.submitted = false;
      }
      this.clearSubmitError();
    });
  }

  get photosArray(): FormArray {
    return this.form.get('photos') as FormArray;
  }

  get alternativeNamesArray(): FormArray {
    return this.form.get('alternativeNames') as FormArray;
  }

  get photoControls(): FormControl[] {
    return this.photosArray.controls as FormControl[];
  }

  get alternativeNameControls(): FormControl[] {
    return this.alternativeNamesArray.controls as FormControl[];
  }

  createAlternativeNameControl(): FormControl {
    return this.fb.control('', [Validators.maxLength(120)]);
  }

  addAlternativeName(): void {
    if (this.alternativeNamesArray.length >= 3) {
      return;
    }
    this.alternativeNamesArray.push(this.createAlternativeNameControl());
  }

  removeAlternativeName(index: number): void {
    if (this.alternativeNamesArray.length <= 1) {
      this.alternativeNamesArray.at(0).reset('');
      return;
    }
    this.alternativeNamesArray.removeAt(index);
  }

  private validateAndAddFiles(files: File[]): void {
    this.photoErrors = [];
    this.clearSubmitError();

    Array.from(files).forEach((file) => {
      if (!file.type.startsWith('image/')) {
        this.photoErrors.push(`"${file.name}" is not a valid image file.`);
        return;
      }

      const fileType = file.type.toLowerCase();
      if (!ALLOWED_PHOTO_TYPES.includes(fileType) && !fileType.startsWith('image/')) {
        this.photoErrors.push(`"${file.name}" has an unsupported file type.`);
        return;
      }

      if (file.size > MAX_PHOTO_SIZE_BYTES) {
        const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
        this.photoErrors.push(`"${file.name}" is too large (${sizeMB} MB). Max size is 10 MB.`);
        return;
      }

      this.photos.push({
        file,
        url: URL.createObjectURL(file),
      });
      this.photosArray.push(this.fb.control(file.name));
      this.photosArray.setErrors(null);
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    this.validateAndAddFiles(Array.from(input.files));
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (!files || files.length === 0) {
      return;
    }

    this.validateAndAddFiles(Array.from(files));
  }

  removePhoto(index: number): void {
    const photo = this.photos[index];
    if (photo?.url) {
      URL.revokeObjectURL(photo.url);
    }
    this.photos.splice(index, 1);
    this.photosArray.removeAt(index);
    this.clearSubmitError();

    if (this.photosArray.length === 0) {
      this.photosArray.setErrors({ required: true });
    }
  }

  replacePhoto(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    this.photoErrors = [];
    this.clearSubmitError();
    const file = input.files[0];

    if (!file.type.startsWith('image/')) {
      this.photoErrors.push(`"${file.name}" is not a valid image file.`);
      input.value = '';
      return;
    }

    const fileType = file.type.toLowerCase();
    if (!ALLOWED_PHOTO_TYPES.includes(fileType)) {
      this.photoErrors.push(`"${file.name}" has an unsupported file type. Supported: JPG, PNG, HEIC, WebP.`);
      input.value = '';
      return;
    }

    if (file.size > MAX_PHOTO_SIZE_BYTES) {
      const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
      this.photoErrors.push(`"${file.name}" is too large (${sizeMB} MB). Max size is 10 MB.`);
      input.value = '';
      return;
    }

    const oldPhoto = this.photos[index];
    if (oldPhoto?.url) {
      URL.revokeObjectURL(oldPhoto.url);
    }

    this.photos[index] = {
      file,
      url: URL.createObjectURL(file),
    };
    this.photosArray.at(index).setValue(file.name);

    input.value = '';
  }

  clearSubmitError(): void {
    this.submitError = '';
  }

  ngOnDestroy(): void {
    this.photos.forEach((photo) => {
      if (photo.url) {
        URL.revokeObjectURL(photo.url);
      }
    });
  }

  submit(): void {
    this.submitted = true;
    this.form.markAllAsTouched();
    this.clearSubmitError();
    this.photoErrors = [];

    if (this.form.invalid || this.photos.length === 0) {
      if (this.photos.length === 0) {
        this.photosArray.setErrors({ required: true });
      }
      return;
    }

    this.uploading = true;
    this.uploadProgress = 0;

    const formData = new FormData();
    
    // Add images
    this.photos.forEach((photo) => {
      formData.append('images', photo.file);
    });

    // Add form fields mapping to backend expectations
    formData.append('churchName', this.form.get('churchName')?.value || '');
    
    // Build location info from locationInfo field
    const locationInfo = this.form.get('locationInfo')?.value || '';
    formData.append('title', locationInfo); // Use location details as title or could combine
    formData.append('description', this.form.get('description')?.value || '');
    formData.append('historicalNotes', this.form.get('notes')?.value || '');
    
    if (this.form.get('town')?.value) formData.append('town', this.form.get('town')?.value);
    if (this.form.get('region')?.value) formData.append('region', this.form.get('region')?.value);
    if (this.form.get('country')?.value) formData.append('country', this.form.get('country')?.value);
    
    const lat = this.form.get('gpsLat')?.value;
    const lng = this.form.get('gpsLng')?.value;
    if (lat) formData.append('latitude', lat.toString());
    if (lng) formData.append('longitude', lng.toString());
    
    if (this.form.get('saintName')?.value) formData.append('saintName', this.form.get('saintName')?.value);
    if (this.form.get('category')?.value) formData.append('category', this.form.get('category')?.value);
    
    const dates = this.form.get('dates')?.value;
    if (dates) formData.append('capturedDate', dates);
    
    const attrPref = this.form.get('attributionPreference')?.value || 'display-name';
    formData.append('attributionPreference', attrPref === 'anonymous' ? 'ANONYMOUS' : 'DISPLAY_NAME');
    
    if (attrPref === 'display-name' && this.form.get('attributionName')?.value) {
      formData.append('contributorDisplayName', this.form.get('attributionName')?.value);
    }
    
    if (this.form.get('contactEmail')?.value) formData.append('contactEmail', this.form.get('contactEmail')?.value);
    
    formData.append('consent', this.form.get('permissionConfirmation')?.value ? 'true' : 'false');

    this.contributionService.submitContribution(formData).subscribe({
      next: (progress) => {
        this.uploadProgress = progress.progress;
        if (progress.response) {
          this.uploading = false;
          const ref = progress.response.referenceCode;
          this.router.navigate(['/contribute/success'], { queryParams: { ref } });
        }
      },
      error: (err) => {
        console.error('Submission failed', err);
        this.uploading = false;
        this.uploadProgress = 0;
        if (err.error?.message) {
          this.submitError = err.error.message;
        } else if (err.message) {
          this.submitError = err.message;
        } else {
          this.submitError = 'Failed to submit contribution. Please try again.';
        }
        this.submitted = true;
      }
    });
  }
}
