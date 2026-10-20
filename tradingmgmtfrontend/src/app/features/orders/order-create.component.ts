import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { CustomerService } from '../../core/services/customer.service';
import { ShareSecurityService } from '../../core/services/security.service';
import { Customer } from '../../core/models/customer.model';
import { ShareSecurity } from '../../core/models/security.model';
import { OrderType } from '../../core/models/order.model';

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './order-create.component.html',
})
export class OrderCreateComponent implements OnInit {
  customers: Customer[] = [];
  securities: ShareSecurity[] = [];
  submitting = false;
  error: string | null = null;

  form: {
    orderType: OrderType;
    buyerCustomerId: string;
    sellerCustomerId: string;
    securityId: string;
    quantity: number | null;
    pricePerShare: number | null;
    orderDate: string;
    notes: string;
  } = {
    orderType: 'BUY',
    buyerCustomerId: '',
    sellerCustomerId: '',
    securityId: '',
    quantity: null,
    pricePerShare: null,
    orderDate: new Date().toISOString().slice(0, 10),
    notes: '',
  };

  constructor(
    private orderService: OrderService,
    private customerService: CustomerService,
    private securityService: ShareSecurityService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.customerService.list().subscribe((c) => (this.customers = c));
    this.securityService.list().subscribe((s) => (this.securities = s));
  }

  get grossAmount(): number | null {
    if (!this.form.quantity || !this.form.pricePerShare) return null;
    return this.form.quantity * this.form.pricePerShare;
  }

  get canSubmit(): boolean {
    return !!(this.form.securityId && this.form.quantity && this.form.pricePerShare);
  }

  submit(): void {
    if (!this.canSubmit) return;
    this.submitting = true;
    this.orderService
      .create({
        orderType: this.form.orderType,
        buyerCustomerId: this.form.buyerCustomerId || undefined,
        sellerCustomerId: this.form.sellerCustomerId || undefined,
        securityId: this.form.securityId,
        quantity: this.form.quantity as number,
        pricePerShare: this.form.pricePerShare as number,
        orderDate: this.form.orderDate,
        notes: this.form.notes || undefined,
      })
      .subscribe({
        next: (order) => this.router.navigate(['/orders', order.id]),
        error: () => {
          this.error = 'Could not create the order. Check all required fields are filled in.';
          this.submitting = false;
        },
      });
  }
}
