import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { I18nService } from './i18n.service';

type LegalPageMode = 'impressum' | 'datenschutz';

@Component({
  selector: 'app-legal-page',
  imports: [CommonModule],
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.scss'
})
export class LegalPageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly i18n = inject(I18nService);

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
}
