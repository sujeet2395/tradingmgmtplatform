import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { TransactionService } from '../../core/services/transaction.service';
import { AuditService } from '../../core/services/audit.service';
import { Order } from '../../core/models/order.model';
import { AuditLog } from '../../core/models/audit-log.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './order-detail.component.html',
})
export class OrderDetailComponent implements OnInit {
  order: Order | null = null;
  auditLog: AuditLog[] = [];
  transactionId: string | null = null;
  loading = true;
  error: string | null = null;
  busy = false;

  showRejectForm = false;
  rejectReason = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService,
    private transactionService: TransactionService,
    private auditService: AuditService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.load(id);
  }

  private load(id: string): void {
    this.loading = true;
    this.orderService.get(id).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
        this.busy = false;
        this.error = null;
        this.auditService.history('Order', order.id).subscribe((log) => (this.auditLog = log));
        if (order.status === 'ORDER_CONFIRMED') {
          this.transactionService.getByOrder(order.id).subscribe({
            next: (txn) => (this.transactionId = txn.id),
            error: () => {},
          });
        }
      },
      error: () => {
        this.error = 'Could not load this order.';
        this.loading = false;
      },
    });
  }

  verify(): void {
    if (!this.order) return;
    this.busy = true;
    this.orderService.verify(this.order.id).subscribe({
      next: () => this.load(this.order!.id),
      error: () => {
        this.error = 'Could not verify the order — check its current status.';
        this.busy = false;
      },
    });
  }

  confirm(): void {
    if (!this.order) return;
    this.busy = true;
    this.orderService.confirm(this.order.id).subscribe({
      next: () => this.load(this.order!.id),
      error: () => {
        this.error = 'Could not confirm the order — check its current status.';
        this.busy = false;
      },
    });
  }

  reject(): void {
    if (!this.order) return;
    this.busy = true;
    this.orderService.reject(this.order.id, this.rejectReason).subscribe({
      next: () => {
        this.showRejectForm = false;
        this.load(this.order!.id);
      },
      error: () => {
        this.error = 'Could not reject the order.';
        this.busy = false;
      },
    });
  }

  goToTransaction(): void {
    if (this.transactionId) this.router.navigate(['/transactions', this.transactionId]);
  }
}
