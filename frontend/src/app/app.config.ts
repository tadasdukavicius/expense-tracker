import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient } from '@angular/common/http';
import { ErrorHandler } from '@angular/core';
import { withInterceptors } from '@angular/common/http';
import { activityInterceptor } from './core/interceptors/activity.interceptor';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { AppErrorHandler } from './core/app-error-handler';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([activityInterceptor])),
    provideRouter(routes),
    provideCharts(withDefaultRegisterables()),
    { provide: ErrorHandler, useClass: AppErrorHandler },
  ]
};
