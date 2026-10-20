import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { CompanyListComponent } from './features/companies/company-list.component';
import { SecurityListComponent } from './features/securities/security-list.component';
import { CustomerListComponent } from './features/customers/customer-list.component';
import { OrderListComponent } from './features/orders/order-list.component';
import { OrderCreateComponent } from './features/orders/order-create.component';
import { OrderDetailComponent } from './features/orders/order-detail.component';
import { TransactionDetailComponent } from './features/transactions/transaction-detail.component';
import { CustomerDetailComponent } from './features/customers/customer-detail.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'orders', component: OrderListComponent },
  { path: 'orders/new', component: OrderCreateComponent },
  { path: 'orders/:id', component: OrderDetailComponent },
  { path: 'transactions/:id', component: TransactionDetailComponent },
  { path: 'customers', component: CustomerListComponent },
  { path: 'customers/:id', component: CustomerDetailComponent },
  { path: 'companies', component: CompanyListComponent },
  { path: 'securities', component: SecurityListComponent },
  { path: '**', redirectTo: '' },
];
