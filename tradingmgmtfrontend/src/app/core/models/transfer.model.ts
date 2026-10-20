export type TransferStatus =
  | 'TRANSFER_PENDING'
  | 'DOCUMENTS_PENDING'
  | 'DOCUMENTS_VERIFIED'
  | 'TRANSFER_INITIATED'
  | 'TRANSFER_SUBMITTED'
  | 'TRANSFER_IN_PROCESS'
  | 'TRANSFER_COMPLETED'
  | 'TRANSFER_REJECTED';

export interface ShareTransfer {
  id: string;
  quantity: number;
  status: TransferStatus;
  initiatedDate?: string;
  submittedDate?: string;
  completedDate?: string;
  rejectionReason?: string;
}
