export interface Company {
  id: string;
  legalName: string;
  cin?: string;
  status: string;
  createdAt: string;
}

export interface CompanyRequest {
  legalName: string;
  cin?: string;
  status?: string;
}
