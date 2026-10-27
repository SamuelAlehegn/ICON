import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';

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
    MatRadioModule,
    MatSelectModule,
  ],
  templateUrl: './contribute.component.html',
  styleUrl: './contribute.component.scss',
})
export class ContributeComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);

  form!: FormGroup;
  submitted = false;
  photos: PhotoPreview[] = [];
  isDragOver = false;
  photoErrors: string[] = [];

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
    let hasValidFiles = false;

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
      hasValidFiles = true;
    });

    if (this.photosArray.length > 0) {
      this.photosArray.setErrors(null);
    }
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

    if (this.form.invalid || this.photos.length === 0) {
      if (this.photos.length === 0) {
        this.photosArray.setErrors({ required: true });
      }
      return;
    }

    // Submission will be wired to backend once available
  }
}
