import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

type LegalPageMode = 'impressum' | 'datenschutz';

@Component({
  selector: 'app-legal-page',
  imports: [CommonModule],
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.scss'
})
export class LegalPageComponent {
  private readonly route = inject(ActivatedRoute);

  protected readonly mode = computed<LegalPageMode>(() => {
    return this.route.snapshot.data['legalPageMode'] === 'impressum' ? 'impressum' : 'datenschutz';
  });

  protected readonly title = computed(() =>
    this.mode() === 'impressum' ? 'Impressum' : 'Datenschutz'
  );

  protected readonly summary = computed(() =>
    this.mode() === 'impressum'
      ? 'Company and contact details for legal disclosure.'
      : 'How personal data is processed in this booking system.'
  );
}
