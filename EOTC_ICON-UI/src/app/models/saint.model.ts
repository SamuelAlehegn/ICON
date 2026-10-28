export interface SaintSummary {
  id: string;
  slug: string;
  canonicalName: string;
  amharicName?: string | null;
  geezName?: string | null;
  category?: string | null;
  verified: boolean;
  iconCount?: number | null;
}

export interface SaintDetail {
  id: string;
  slug: string;
  canonicalName: string;
  amharicName?: string | null;
  geezName?: string | null;
  category?: string | null;
  description?: string | null;
  verified: boolean;
  status?: string | null;
  aliases?: string[] | null;
  iconCount: number;
  depictions?: IconSummary[] | null;
  churches?: ChurchSummary[] | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

import { IconSummary } from './icon.model';
import { ChurchSummary } from './church.model';
