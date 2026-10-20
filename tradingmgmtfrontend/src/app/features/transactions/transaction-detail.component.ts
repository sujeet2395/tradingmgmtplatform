import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TransactionService } from '../../core/services/transaction.service';
import { PaymentService } from '../../core/services/payment.service';
import { TransferService } from '../../core/services/transfer.service';
import { AuditService } from '../../core/services/audit.service';
import { Transaction } from '../../core/models/transaction.model';
import { Payment } from '../../core/models/payment.model';
import { ShareTransfer } from '../../core/models/transfer.model';
import { AuditLog } from '../../core/models/audit-log.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { DocumentsPanelComponent } from '../../shared/documents-panel/documents-panel.component';

@Component({
  selector: 'app-transaction-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent, DocumentsPanelComponent],
  templateUrl: './transaction-detail.component.html',
})
export class TransactionDetailComponent implements OnInit {
  transaction: Transaction | null = null;
  payment: Payment | null = null;
  transfer: ShareTransfer | null = null;
  auditLog: AuditLog[] = [];
  loading = true;
  error: string | null = null;
  busy = false;

  paymentForm = { amount: null as number | null };
  chequeForm = { chequeNumber: '', bankName: '' };
  showRejectTransferForm = false;
  rejectTransferReason = '';

  constructor(
    private route: ActivatedRoute,
    private transactionService: TransactionService,
    private paymentService: PaymentService,
    private transferService: TransferService,
    private auditService: AuditService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.load(id);
  }

  private load(id: string): void {
    this.loading = true;
    this.transactionService.get(id).subscribe({
      next: (txn) => {
        this.transaction = txn;
        this.paymentForm.amount = txn.totalAmount;
        this.loading = false;
        this.busy = false;
        this.error = null;
        this.paymentService.listByTransaction(txn.id).subscribe((payments) => {
          this.payment = payments.length ? payments[payments.length - 1] : null;
        });
        this.transferService.listByTransaction(txn.id).subscribe((transfers) => {
          this.transfer = transfers.length ? transfers[transfers.length - 1] : null;
        });
        this.auditService.history('Transaction', txn.id).subscribe((log) => (this.auditLog = log));
      },
      error: () => {
        this.error = 'Could not load this transaction.';
        this.loading = false;
      },
    });
  }

  private reload(): void {
    if (this.transaction) this.load(this.transaction.id);
  }

  private fail(message: string): void {
    this.error = message;
    this.busy = false;
  }

  initiatePayment(): void {
    if (!this.transaction || !this.paymentForm.amount) return;
    this.busy = true;
    this.paymentService.initiate(this.transaction.id, { amount: this.paymentForm.amount }).subscribe({
      next: () => this.reload(),
      error: () => this.fail('Could not initiate the payment.'),
    });
  }

  markChequeReceived(): void {
    if (!this.payment || !this.chequeForm.chequeNumber.trim()) return;
    this.busy = true;
    this.paymentService
      .chequeReceived(this.payment.id, { chequeNumber: this.chequeForm.chequeNumber, bankName: this.chequeForm.bankName || undefined })
      .subscribe({
        next: () => this.reload(),
        error: () => this.fail('Could not record the cheque.'),
      });
  }

  markChequeDeposited(): void {
    if (!this.payment) return;
    this.busy = true;
    this.paymentService.deposit(this.payment.id).subscribe({
      next: () => this.reload(),
      error: () => this.fail('Could not mark the cheque as deposited.'),
    });
  }

  verifyPayment(): void {
    if (!this.payment) return;
    this.busy = true;
    this.paymentService.verify(this.payment.id).subscribe({
      next: () => this.reload(),
      error: () => this.fail('Could not verify the payment.'),
    });
  }

  initiateTransfer(): void {
    if (!this.transaction) return;
    this.busy = true;
    this.transferService.initiate(this.transaction.id).subscribe({
      next: () => this.reload(),
      error: () => this.fail('Could not initiate the share transfer.'),
    });
  }

  submitTransfer(): void {
    if (!this.transfer) return;
    this.busy = true;
    this.transferService.submit(this.transfer.id).subscribe({
      next: () => this.reload(),
      error: () => this.fail('Could not submit the share transfer.'),
    });
  }

  completeTransfer(): void {
    if (!this.transfer) return;
    this.busy = true;
    this.transferService.complete(this.transfer.id).subscribe({
      next: () => this.reload(),
      error: () => this.fail('Could not complete the share transfer.'),
    });
  }

  rejectTransfer(): void {
    if (!this.transfer || !this.rejectTransferReason.trim()) return;
    this.busy = true;
    this.transferService.reject(this.transfer.id, this.rejectTransferReason).subscribe({
      next: () => {
        this.showRejectTransferForm = false;
        this.reload();
      },
      error: () => this.fail('Could not reject the share transfer.'),
    });
  }
}
