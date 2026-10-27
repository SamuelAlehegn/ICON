export interface IconSummary {
  id: string;
  slug: string;
  title: string;
  depictedFigure?: string | null;
  figureCertainty?: string | null;
  saintNames?: string[] | null;
  churchName?: string | null;
  churchSlug?: string | null;
  town?: string | null;
  region?: string | null;
  country?: string | null;
  contributorCredit?: string | null;
  thumbnailUrl?: string | null;
  imageUrl?: string | null;
  altText?: string | null;
}

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

export interface ImageRights {
  rightsHolder?: string | null;
  licenseType?: string | null;
  licenseUrl?: string | null;
  licenseNotes?: string | null;
  attributionText?: string | null;
  copyrightYear?: number | null;
  verified: boolean;
}

export interface IconImage {
  id: string;
  imageUrl?: string | null;
  thumbnailUrl?: string | null;
  altText?: string | null;
  contentType?: string | null;
  fileSizeBytes?: number | null;
  widthPx?: number | null;
  heightPx?: number | null;
  primary: boolean;
  sortOrder: number;
  capturedAt?: string | null;
  rights?: ImageRights | null;
}

export interface IconSubject {
  id: string;
  subjectLabel?: string | null;
  figureCertainty?: string | null;
  primary: boolean;
  sortOrder: number;
  saint?: SaintSummary | null;
}

export interface IconDetail {
  id: string;
  slug: string;
  referenceCode?: string | null;
  title: string;
  description?: string | null;
  historicalNotes?: string | null;
  category?: string | null;
  figureCertainty?: string | null;
  captureDate?: string | null;
  artworkDateStart?: string | null;
  artworkDateEnd?: string | null;
  contributorCredit?: string | null;
  publishedAt?: string | null;
  church?: ChurchSummary | null;
  subjects?: IconSubject[] | null;
  images?: IconImage[] | null;
  relatedIcons?: IconSummary[] | null;
  moreFromChurch?: IconSummary[] | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ChurchDetail extends ChurchSummary {
  description?: string | null;
  history?: string | null;
  iconCount?: number | null;
  icons?: IconSummary[] | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
