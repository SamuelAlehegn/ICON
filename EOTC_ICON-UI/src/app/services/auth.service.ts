import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  UserProfile,
  UserRole
} from '../models/auth.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly baseUrl = environment.apiBaseUrl;

  private readonly currentUserState = signal<UserProfile | null>(null);
  private refreshInFlight: Observable<TokenResponse> | null = null;
  private restoreInFlight: Observable<UserProfile | null> | null = null;

  readonly currentUser = this.currentUserState.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserState() !== null);
  readonly role = computed<UserRole | null>(() => this.currentUserState()?.role ?? null);
  readonly isContributor = computed(() => this.role() !== null);
  readonly isModerator = computed(() => {
    const role = this.role();
    return role === 'MODERATOR' || role === 'ADMIN';
  });
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  login(request: LoginRequest): Observable<UserProfile> {
    return this.http.post<TokenResponse>(`${this.baseUrl}/api/v1/auth/login`, request).pipe(
      tap((tokens) => this.tokenStorage.save(tokens)),
      switchMap(() => this.loadProfile()),
      tap((user) => this.currentUserState.set(user))
    );
  }

  register(request: RegisterRequest): Observable<UserProfile> {
    return this.http.post<TokenResponse>(`${this.baseUrl}/api/v1/auth/register`, request).pipe(
      tap((tokens) => this.tokenStorage.save(tokens)),
      switchMap(() => this.loadProfile()),
      tap((user) => this.currentUserState.set(user))
    );
  }

  restoreSession(): Observable<UserProfile | null> {
    const user = this.currentUserState();
    if (user) {
      return of(user);
    }
    if (!this.tokenStorage.hasTokens()) {
      return of(null);
    }
    if (this.restoreInFlight) {
      return this.restoreInFlight;
    }

    this.restoreInFlight = this.fetchProfile().pipe(
      tap((profile) => this.currentUserState.set(profile)),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
          this.clearSession();
        }
        return of(null);
      }),
      finalize(() => {
        this.restoreInFlight = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    return this.restoreInFlight;
  }

  refreshAccessToken(): Observable<TokenResponse> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }

    const refreshToken = this.tokenStorage.refreshToken;
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token is available'));
    }

    this.refreshInFlight = this.http
      .post<TokenResponse>(`${this.baseUrl}/api/v1/auth/refresh`, { refreshToken })
      .pipe(
        tap((tokens) => this.tokenStorage.save(tokens)),
        finalize(() => {
          this.refreshInFlight = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );

    return this.refreshInFlight;
  }

  logout(): Observable<void> {
    const refreshToken = this.tokenStorage.refreshToken;
    this.clearSession();
    if (!refreshToken) {
      return of(void 0);
    }

    return this.http
      .post<void>(`${this.baseUrl}/api/v1/auth/logout`, { refreshToken })
      .pipe(catchError(() => of(void 0)));
  }

  clearSession(): void {
    this.currentUserState.set(null);
    this.tokenStorage.clear();
  }

  getAccessToken(): string | null {
    return this.tokenStorage.accessToken;
  }

  getRefreshToken(): string | null {
    return this.tokenStorage.refreshToken;
  }

  hasRole(...roles: UserRole[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  private loadProfile(): Observable<UserProfile> {
    return this.fetchProfile().pipe(
      catchError((error: unknown) => {
        this.clearSession();
        return throwError(() => error);
      })
    );
  }

  private fetchProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/api/v1/auth/me`);
  }
}
