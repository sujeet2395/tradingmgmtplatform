import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models/order.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  error: string | null = null;

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.orderService.list().subscribe({
      next: (orders) => {
        this.orders = orders.sort((a, b) => b.orderDate.localeCompare(a.orderDate));
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load orders. Is the backend running?';
        this.loading = false;
      },
    });
  }

  get openOrders(): number {
    return this.orders.filter((o) => o.status === 'ORDER_RECEIVED' || o.status === 'ORDER_VERIFIED').length;
  }

  get confirmedOrders(): number {
    return this.orders.filter((o) => o.status === 'ORDER_CONFIRMED').length;
  }

  get rejectedOrders(): number {
    return this.orders.filter((o) => o.status === 'ORDER_REJECTED' || o.status === 'ORDER_CANCELLED').length;
  }

  get recentOrders(): Order[] {
    return this.orders.slice(0, 8);
  }
}
