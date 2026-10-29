import { ChurchSummary } from './church.model';
import { IconImage, IconSubject } from './icon.model';
import { AttributionPreference, ReviewHistory, SubmissionStatus } from './submission.model';

export type ReviewAction = 'APPROVE' | 'REJECT' | 'REQUEST_CORRECTION';

export interface AdminSubmissionSummary {
  id: string;
  referenceCode: string;
  title: string;
  depictedFigure?: string | null;
  churchName?: string | null;
  status: SubmissionStatus;
  imageCount: number;
  submittedAt?: string | null;
  updatedAt?: string | null;
}

export interface AdminSubmissionDetail {
  id: string;
  referenceCode: string;
  slug: string;
  title: string;
  status: SubmissionStatus;
  description?: string | null;
  historicalNotes?: string | null;
  category?: string | null;
  figureCertainty?: string | null;
  captureDate?: string | null;
  artworkDateStart?: string | null;
  artworkDateEnd?: string | null;
  contributorDisplayName?: string | null;
  attributionPreference: AttributionPreference;
  contributorCredit?: string | null;
  contactEmail?: string | null;
  publishedAt?: string | null;
  church?: ChurchSummary | null;
  subjects?: IconSubject[] | null;
  images?: IconImage[] | null;
  reviews?: ReviewHistory[] | null;
  submittedAt?: string | null;
  updatedAt?: string | null;
}

export interface ReviewDecisionPayload {
  action: ReviewAction;
  notes?: string;
}

export interface ReviewDecisionResponse {
  submissionId: string;
  action: ReviewAction;
  status: SubmissionStatus;
  message: string;
  timestamp: string;
}
