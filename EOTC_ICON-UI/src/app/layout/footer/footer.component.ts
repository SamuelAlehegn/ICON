import { Component } from '@angular/core';
import { MatDividerModule } from '@angular/material/divider';
import { RouterLink } from '@angular/router';

interface FooterLink {
  label: string;
  path: string;
}

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, MatDividerModule],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {
  readonly year = new Date().getFullYear();

  readonly exploreLinks: FooterLink[] = [
    { label: 'Browse Icons', path: '/icons' },
    { label: 'Churches', path: '/churches' },
    { label: 'Saints & Figures', path: '/saints' },
    { label: 'Map', path: '/map' },
    { label: 'Contribute', path: '/contribute' }
  ];

  readonly projectLinks: FooterLink[] = [
    { label: 'About', path: '/about' },
    { label: 'Contribution Guidelines', path: '/guidelines' },
    { label: 'Privacy', path: '/privacy' },
    { label: 'Image Rights', path: '/image-rights' },
    { label: 'Contact', path: '/contact' }
  ];
}
