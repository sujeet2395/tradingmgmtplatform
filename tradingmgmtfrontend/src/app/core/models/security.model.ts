import { Company } from './company.model';

export interface ShareSecurity {
  id: string;
  company: Company;
  securityType: string;
  isin?: string;
  faceValue?: number;
  shareClass?: string;
  referencePrice?: number;
}

export interface ShareSecurityRequest {
  companyId: string;
  securityType?: string;
  isin?: string;
  faceValue?: number;
  shareClass?: string;
  referencePrice?: number;
}
