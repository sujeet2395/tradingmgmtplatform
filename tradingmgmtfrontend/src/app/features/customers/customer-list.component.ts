import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CustomerService } from '../../core/services/customer.service';
import { Customer } from '../../core/models/customer.model';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './customer-list.component.html',
})
export class CustomerListComponent implements OnInit {
  customers: Customer[] = [];
  loading = true;
  error: string | null = null;
  showForm = false;
  submitting = false;

  form = { fullName: '', phone: '', email: '' };

  constructor(private customerService: CustomerService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.customerService.list().subscribe({
      next: (customers) => {
        this.customers = customers;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load customers.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (!this.form.fullName.trim()) return;
    this.submitting = true;
    this.customerService
      .create({ fullName: this.form.fullName, phone: this.form.phone || undefined, email: this.form.email || undefined })
      .subscribe({
        next: () => {
          this.form = { fullName: '', phone: '', email: '' };
          this.showForm = false;
          this.submitting = false;
          this.load();
        },
        error: () => {
          this.error = 'Could not create the customer.';
          this.submitting = false;
        },
      });
  }
}
