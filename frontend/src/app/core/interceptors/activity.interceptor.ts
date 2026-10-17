import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { HttpActivityService } from '../services/http-activity.service';

export const activityInterceptor: HttpInterceptorFn = (request, next) => {
  const activity = inject(HttpActivityService);
  activity.begin();

  return next(request).pipe(finalize(() => activity.end()));
};