import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [MatToolbarModule],
  template: `
    <mat-toolbar class="footer">
      <span>&copy; {{ year }} EOTC Icon Archive</span>
    </mat-toolbar>
  `,
  styles: [`
    .footer { justify-content: center; font-size: 14px; }
  `]
})
export class FooterComponent {
  year = new Date().getFullYear();
}
