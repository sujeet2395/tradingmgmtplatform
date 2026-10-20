import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CompanyService } from '../../core/services/company.service';
import { Company } from '../../core/models/company.model';

@Component({
  selector: 'app-company-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './company-list.component.html',
})
export class CompanyListComponent implements OnInit {
  companies: Company[] = [];
  loading = true;
  error: string | null = null;
  showForm = false;
  submitting = false;

  form = { legalName: '', cin: '' };

  constructor(private companyService: CompanyService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.companyService.list().subscribe({
      next: (companies) => {
        this.companies = companies;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load companies.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (!this.form.legalName.trim()) return;
    this.submitting = true;
    this.companyService.create({ legalName: this.form.legalName, cin: this.form.cin || undefined }).subscribe({
      next: () => {
        this.form = { legalName: '', cin: '' };
        this.showForm = false;
        this.submitting = false;
        this.load();
      },
      error: () => {
        this.error = 'Could not create the company. Check the legal name is unique.';
        this.submitting = false;
      },
    });
  }
}
