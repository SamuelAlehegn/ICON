import { UserProfile, UserRole } from './auth.model';
import { ChurchSummary } from './church.model';
import { SaintSummary } from './saint.model';

export interface ArchiveStatistics {
  publishedIcons: number;
  churches: number;
  saints: number;
  regions: number;
  contributors: number;
  sampleData: boolean;
}

export interface AdminAuditEntry {
  id: string;
  createdAt: string;
  actorId?: string | null;
  actorName: string;
  action: string;
  entityType: string;
  entityId?: string | null;
  detail?: string | null;
  ipAddress?: string | null;
}

export interface ChurchAdminPayload extends Pick<ChurchSummary, 'name' | 'region' | 'town' | 'country' | 'latitude' | 'longitude'> {
  description?: string | null;
}

export interface SaintAdminPayload extends Pick<SaintSummary, 'canonicalName' | 'amharicName' | 'geezName' | 'category' | 'verified'> {
  description?: string | null;
}

export interface AdminRoleUpdate { role: UserRole; }
export type AdminUser = UserProfile;
