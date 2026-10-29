import { Injectable } from '@angular/core';
import { TokenResponse } from '../models/auth.model';

const ACCESS_TOKEN_KEY = 'eotc.auth.accessToken';
const REFRESH_TOKEN_KEY = 'eotc.auth.refreshToken';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  private get storage(): Storage | null {
    return typeof window === 'undefined' ? null : window.localStorage;
  }

  get accessToken(): string | null {
    return this.storage?.getItem(ACCESS_TOKEN_KEY) ?? null;
  }

  get refreshToken(): string | null {
    return this.storage?.getItem(REFRESH_TOKEN_KEY) ?? null;
  }

  hasTokens(): boolean {
    return !!this.accessToken || !!this.refreshToken;
  }

  save(tokens: TokenResponse): void {
    this.storage?.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    this.storage?.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  }

  clear(): void {
    this.storage?.removeItem(ACCESS_TOKEN_KEY);
    this.storage?.removeItem(REFRESH_TOKEN_KEY);
  }
}
