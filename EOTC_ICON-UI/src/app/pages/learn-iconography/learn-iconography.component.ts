import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-learn-iconography',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  templateUrl: './learn-iconography.component.html',
  styleUrl: './learn-iconography.component.scss'
})
export class LearnIconographyComponent {}