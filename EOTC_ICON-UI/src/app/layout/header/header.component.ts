import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, MatToolbarModule, MatButtonModule],
  template: `
    <mat-toolbar color="primary">
      <span>EOTC Icon Archive</span>
      <span class="spacer"></span>
      <a mat-button routerLink="/">Home</a>
    </mat-toolbar>
  `,
  styles: [`
    .spacer { flex: 1 1 auto; }
  `]
})
export class HeaderComponent {}
