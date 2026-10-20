import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CustomerService } from '../../core/services/customer.service';
import { Customer } from '../../core/models/customer.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { DocumentsPanelComponent } from '../../shared/documents-panel/documents-panel.component';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, DocumentsPanelComponent],
  templateUrl: './customer-detail.component.html',
})
export class CustomerDetailComponent implements OnInit {
  customer: Customer | null = null;
  error: string | null = null;

  constructor(private route: ActivatedRoute, private customerService: CustomerService) {}

  ngOnInit(): void {
    this.customerService.get(this.route.snapshot.paramMap.get('id')!).subscribe({
      next: (c) => (this.customer = c),
      error: () => (this.error = 'Could not load this customer.'),
    });
  }

  setKyc(verify: boolean): void {
    const id = this.customer!.id;
    const call = verify ? this.customerService.verifyKyc(id) : this.customerService.revokeKyc(id);
    call.subscribe({
      next: (c) => { this.customer = c; this.error = null; },
      error: (e) => (this.error = e.error?.message ?? 'Could not update KYC.'),
    });
  }
}