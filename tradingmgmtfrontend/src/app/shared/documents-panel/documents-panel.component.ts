import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../core/services/document.service';
import { AppDocument, DocumentCategory } from '../../core/models/document.model';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';

@Component({
  selector: 'app-documents-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './documents-panel.component.html',
})
export class DocumentsPanelComponent implements OnInit {
  @Input({ required: true }) entityType!: string;
  @Input({ required: true }) entityId!: string;
  @Input() categories: DocumentCategory[] = ['OTHER'];
  @Input() title = 'Documents';

  docs: AppDocument[] = [];
  category!: DocumentCategory;
  file: File | null = null;
  error: string | null = null;
  busy = false;
  rejectingId: string | null = null;
  rejectNote = '';

  constructor(private documentService: DocumentService) {}

  ngOnInit(): void {
    this.category = this.categories[0];
    this.load();
  }

  load(): void {
    this.documentService.list(this.entityType, this.entityId).subscribe((d) => (this.docs = d));
  }

  href(d: AppDocument): string { return this.documentService.downloadUrl(d.id); }

  onFile(e: Event): void {
    this.file = (e.target as HTMLInputElement).files?.[0] ?? null;
  }

  upload(input: HTMLInputElement): void {
    if (!this.file) return;
    this.busy = true;
    this.error = null;
    this.documentService.upload(this.file, this.category, this.entityType, this.entityId).subscribe({
      next: () => { this.file = null; input.value = ''; this.busy = false; this.load(); },
      error: (e) => { this.error = e.error?.message ?? 'Upload failed (max 10 MB).'; this.busy = false; },
    });
  }

  verify(d: AppDocument): void {
    this.documentService.verify(d.id).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Could not verify the document.'),
    });
  }

  reject(d: AppDocument): void {
    if (!this.rejectNote.trim()) return;
    this.documentService.reject(d.id, this.rejectNote).subscribe({
      next: () => { this.rejectingId = null; this.rejectNote = ''; this.load(); },
      error: () => (this.error = 'Could not reject the document.'),
    });
  }
}