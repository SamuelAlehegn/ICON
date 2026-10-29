import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';

const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirmation = group.get('confirmPassword')?.value;
  return password === confirmation ? null : { passwordsMismatch: true };
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group(
    {
      displayName: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordsMatch }
  );

  submitting = false;
  hidePassword = true;
  hideConfirmation = true;
  error = '';

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl('/account');
    }
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmationVisibility(): void {
    this.hideConfirmation = !this.hideConfirmation;
  }

  submit(): void {
    if (this.submitting) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { displayName, email, password } = this.form.getRawValue();
    this.submitting = true;
    this.error = '';

    this.auth
      .register({ displayName: displayName.trim(), email: email.trim(), password })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/account'),
        error: (error: unknown) => {
          this.error = this.errorMessage(error);
        }
      });
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Unable to reach the server. Check your connection and try again.';
      }

      const body = error.error as Partial<ApiError> | null;
      const fieldMessage = body?.fieldErrors?.[0]?.message;
      if (typeof fieldMessage === 'string' && fieldMessage.trim()) {
        return fieldMessage;
      }
      if (body?.message && body.message !== 'An unexpected error occurred') {
        return body.message;
      }
    }

    return 'Unable to create your account. Please try again.';
  }
}
