import { ErrorHandler, Injectable, inject } from '@angular/core';
import { AppErrorService } from './services/app-error.service';

@Injectable()
export class AppErrorHandler implements ErrorHandler {
  private readonly errors = inject(AppErrorService);

  handleError(error: unknown): void {
    console.error(error);
    this.errors.report('An unexpected application error occurred. Please try again.');
  }
}