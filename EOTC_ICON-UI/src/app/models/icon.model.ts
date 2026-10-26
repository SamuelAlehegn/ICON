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

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
