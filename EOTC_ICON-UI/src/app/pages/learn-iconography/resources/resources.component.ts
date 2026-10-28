import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LazyImageDirective } from '../../../directives/lazy-image.directive';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-resources',
  standalone: true,
  imports: [RouterLink, MatButtonModule, LazyImageDirective],
  templateUrl: './resources.component.html',
  styleUrl: './resources.component.scss'
})
export class ResourcesComponent {}