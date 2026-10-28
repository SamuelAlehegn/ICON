import { ChurchSummary } from './church.model';
import { IconImage, IconSubject } from './icon.model';

export type SubmissionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'NEEDS_CORRECTION' | 'ARCHIVED';

export type AttributionPreference = 'DISPLAY_NAME' | 'ANONYMOUS';

export interface ReviewHistory {
  id: string;
  action: string;
  fromStatus?: SubmissionStatus | null;
  toStatus?: SubmissionStatus | null;
  notes?: string | null;
  reviewerId?: string | null;
  reviewerDisplayName?: string | null;
  createdAt: string;
}

export interface AccountSubmission {
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
  moderatorNotes?: string | null;
  publishedAt?: string | null;
  church?: ChurchSummary | null;
  subjects?: IconSubject[] | null;
  images?: IconImage[] | null;
  reviews?: ReviewHistory[] | null;
  submittedAt?: string | null;
  updatedAt?: string | null;
}

export interface UpdateSubmissionPayload {
  title?: string;
  description?: string | null;
  historicalNotes?: string | null;
  category?: string | null;
  captureDate?: string | null;
  artworkDateStart?: string | null;
  artworkDateEnd?: string | null;
  attributionPreference?: AttributionPreference;
  contributorDisplayName?: string | null;
  contactEmail?: string | null;
}

export interface SubmissionRequestResponse {
  submissionId: string;
  action: string;
  status: SubmissionStatus;
  message: string;
  timestamp: string;
}
