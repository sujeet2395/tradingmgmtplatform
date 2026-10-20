export type DocumentCategory = 'KYC' | 'CHEQUE_EVIDENCE' | 'PAYMENT_PROOF'
  | 'TRANSFER_FORM' | 'TRANSFER_CONFIRMATION' | 'AGREEMENT' | 'OTHER';

export interface DocumentVersion {
  id: string;
  versionNumber: number;
  fileName: string;
  uploadedAt: string;
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
  verificationNote?: string;
}

export interface AppDocument {
  id: string;
  category: DocumentCategory;
  relatedEntityType: string;
  relatedEntityId: string;
  currentVersion: DocumentVersion;
}