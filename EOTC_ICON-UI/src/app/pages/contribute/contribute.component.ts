import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
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
export class ContributeComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  form!: FormGroup;
  submitted = false;
  photos: File[] = [];

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
    this.alternativeNamesArray.push(this.createAlternativeNameControl());
  }

  removeAlternativeName(index: number): void {
    if (this.alternativeNamesArray.length <= 1) {
      this.alternativeNamesArray.at(0).reset('');
      return;
    }
    this.alternativeNamesArray.removeAt(index);
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    Array.from(input.files).forEach((file) => {
      if (file.type.startsWith('image/')) {
        this.photos.push(file);
        this.photosArray.push(this.fb.control(file.name));
      }
    });

    input.value = '';
  }

  removePhoto(index: number): void {
    this.photos.splice(index, 1);
    this.photosArray.removeAt(index);
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
