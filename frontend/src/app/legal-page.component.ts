import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { I18nService } from './i18n.service';
import { catchError, of } from 'rxjs';

type LegalPageMode = 'impressum' | 'datenschutz';
type LegalField = { label: string; value: string };
type LegalContent = { impressumFields: LegalField[]; datenschutzFields: LegalField[] };

@Component({
  selector: 'app-legal-page',
  imports: [CommonModule],
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.scss'
})
export class LegalPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  protected readonly i18n = inject(I18nService);
  protected legalContent: LegalContent = { impressumFields: [], datenschutzFields: [] };

  protected readonly mode = computed<LegalPageMode>(() => {
    return this.route.snapshot.data['legalPageMode'] === 'impressum' ? 'impressum' : 'datenschutz';
  });

  protected readonly title = computed(() =>
    this.mode() === 'impressum' ? this.i18n.t('legal.title.impressum') : this.i18n.t('legal.title.datenschutz')
  );

  protected readonly summary = computed(() =>
    this.mode() === 'impressum'
      ? this.i18n.t('legal.summary.impressum')
      : this.i18n.t('legal.summary.datenschutz')
  );

  constructor() {
    this.http
      .get<LegalContent>('/api/legal/published')
      .pipe(catchError(() => of({ impressumFields: [], datenschutzFields: [] })))
      .subscribe((content) => {
        this.legalContent = {
          impressumFields: this.normalizeFields(content?.impressumFields),
          datenschutzFields: this.normalizeFields(content?.datenschutzFields)
        };
      });
  }

  protected fieldsForCurrentPage(): LegalField[] {
    return this.mode() === 'impressum' ? this.legalContent.impressumFields : this.legalContent.datenschutzFields;
  }

  private normalizeFields(fields: LegalField[] | null | undefined): LegalField[] {
    const safeFields = Array.isArray(fields) ? fields : [];
    return safeFields
      .map((field) => ({
        label: (field?.label ?? '').trim(),
        value: (field?.value ?? '').trim()
      }))
      .filter((field) => field.label || field.value);
  }
}
