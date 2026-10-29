export type UserRole = 'CONTRIBUTOR' | 'MODERATOR' | 'ADMIN';

export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
  status: UserStatus;
  lastLoginAt?: string | null;
  createdAt?: string | null;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: FieldError[];
}
