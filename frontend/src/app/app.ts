import { RouterLink, RouterOutlet } from '@angular/router';
import { Component, inject, signal } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AppErrorService } from './core/services/app-error.service';
import { HttpActivityService } from './core/services/http-activity.service';

@Component({
  imports: [RouterLink, RouterOutlet, MatProgressBarModule],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  readonly activity = inject(HttpActivityService);
  readonly errors = inject(AppErrorService);
}
