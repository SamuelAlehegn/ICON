import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]]
  });

  submitting = false;
  hidePassword = true;
  error = '';
  returnUrl = '/account';

  ngOnInit(): void {
    this.returnUrl = this.resolveReturnUrl();
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl(this.returnUrl);
    }
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  submit(): void {
    if (this.submitting) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, password } = this.form.getRawValue();
    this.submitting = true;
    this.error = '';

    this.auth
      .login({ email: email.trim(), password })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => void this.router.navigateByUrl(this.returnUrl),
        error: (error: unknown) => {
          this.error = this.errorMessage(error);
        }
      });
  }

  private resolveReturnUrl(): string {
    const requested = this.route.snapshot.queryParamMap.get('returnUrl');
    if (!requested || !requested.startsWith('/') || requested.startsWith('//')) {
      return '/account';
    }
    if (requested.startsWith('/login')) {
      return '/account';
    }
    return requested;
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'Unable to reach the server. Check your connection and try again.';
      }
      if (error.status === 429) {
        return 'Too many sign-in attempts. Please wait a moment and try again.';
      }
      if (error.status === 400 || error.status === 401 || error.status === 403) {
        return 'Invalid email or password.';
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

    return 'Invalid email or password.';
  }
}
