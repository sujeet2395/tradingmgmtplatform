export interface Customer {
  id: string;
  fullName: string;
  phone?: string;
  email?: string;
  address?: string;
  bankAccountName?: string;
  bankIfsc?: string;
  kycVerified: boolean;
}

export interface CustomerRequest {
  fullName: string;
  phone?: string;
  email?: string;
  address?: string;
  bankAccountName?: string;
  bankIfsc?: string;
}
