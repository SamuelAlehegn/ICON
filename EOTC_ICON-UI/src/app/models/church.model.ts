import { IconSummary } from './icon.model';

export interface ChurchSummary {
  id: string;
  slug: string;
  name: string;
  region?: string | null;
  town?: string | null;
  country?: string | null;
  latitude?: number | null;
  longitude?: number | null;
}

export interface ChurchDetail extends ChurchSummary {
  description?: string | null;
  history?: string | null;
  iconCount?: number | null;
  icons?: IconSummary[] | null;
}
