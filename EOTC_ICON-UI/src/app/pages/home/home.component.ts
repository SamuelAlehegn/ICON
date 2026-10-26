import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

interface ArchiveFeature {
  title: string;
  description: string;
}

interface ExplorePath {
  kicker: string;
  label: string;
  description: string;
  path: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  readonly features: ArchiveFeature[] = [
    {
      title: 'Documented with care',
      description:
        'Each record is built around reliable metadata: origin, setting, material, date, and the people who cared for the work.'
    },
    {
      title: 'Attributed with respect',
      description:
        'Figures, churches, and communities are named only when the evidence allows. Unknown details remain honestly unlabeled.'
    },
    {
      title: 'Reviewed before publication',
      description:
        'Contributions are held privately and reviewed by moderators so that images and their rights are handled responsibly.'
    }
  ];

  readonly explorePaths: ExplorePath[] = [
    {
      kicker: 'The collection',
      label: 'Icons',
      description: 'Browse painted icons and religious wall paintings.',
      path: '/icons'
    },
    {
      kicker: 'Places of worship',
      label: 'Churches',
      description: 'Discover the churches and communities that keep these works.',
      path: '/churches'
    },
    {
      kicker: 'Figures of faith',
      label: 'Saints',
      description: 'Explore the saints and figures depicted across the archive.',
      path: '/saints'
    },
    {
      kicker: 'Across Ethiopia',
      label: 'Map',
      description: 'See where documented churches and their collections are located.',
      path: '/map'
    }
  ];
}
