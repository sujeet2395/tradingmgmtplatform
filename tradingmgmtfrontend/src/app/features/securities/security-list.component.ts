import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ShareSecurityService } from '../../core/services/security.service';
import { CompanyService } from '../../core/services/company.service';
import { ShareSecurity } from '../../core/models/security.model';
import { Company } from '../../core/models/company.model';

@Component({
  selector: 'app-security-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './security-list.component.html',
})
export class SecurityListComponent implements OnInit {
  securities: ShareSecurity[] = [];
  companies: Company[] = [];
  loading = true;
  error: string | null = null;
  showForm = false;
  submitting = false;

  form = { companyId: '', isin: '', shareClass: '', referencePrice: null as number | null };

  constructor(
    private securityService: ShareSecurityService,
    private companyService: CompanyService
  ) {}

  ngOnInit(): void {
    this.load();
    this.companyService.list().subscribe((companies) => (this.companies = companies));
  }

  load(): void {
    this.loading = true;
    this.securityService.list().subscribe({
      next: (securities) => {
        this.securities = securities;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load securities.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (!this.form.companyId) return;
    this.submitting = true;
    this.securityService
      .create({
        companyId: this.form.companyId,
        isin: this.form.isin || undefined,
        shareClass: this.form.shareClass || undefined,
        referencePrice: this.form.referencePrice ?? undefined,
      })
      .subscribe({
        next: () => {
          this.form = { companyId: '', isin: '', shareClass: '', referencePrice: null };
          this.showForm = false;
          this.submitting = false;
          this.load();
        },
        error: () => {
          this.error = 'Could not create the security.';
          this.submitting = false;
        },
      });
  }
}
