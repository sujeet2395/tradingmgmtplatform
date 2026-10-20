import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models/order.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './order-list.component.html',
})
export class OrderListComponent implements OnInit {
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
        this.error = 'Could not load orders.';
        this.loading = false;
      },
    });
  }
}
