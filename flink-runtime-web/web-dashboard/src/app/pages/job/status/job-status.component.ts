/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { merge, Subject } from 'rxjs';
import { distinctUntilKeyChanged, takeUntil, tap } from 'rxjs/operators';

import { JobDetailCorrect } from '@flink-runtime-web/interfaces';
import { JobService, StatusService } from '@flink-runtime-web/services';

import { JobLocalService } from '../job-local.service';

@Component({
  selector: 'flink-job-status',
  templateUrl: './job-status.component.html',
  styleUrls: ['./job-status.component.less'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class JobStatusComponent implements OnInit, OnDestroy {
  @Input() public isLoading = true;
  public statusTips: string;
  public jobDetail: JobDetailCorrect;
  public readonly listOfNavigation = [
    {
      path: 'overview',
      title: 'Overview'
    },
    {
      path: 'exceptions',
      title: 'Exceptions'
    },
    {
      path: 'timeline',
      title: 'TimeLine'
    },
    {
      path: 'checkpoints',
      title: 'Checkpoints'
    },
    {
      path: 'configuration',
      title: 'Configuration'
    }
  ];

  public webCancelEnabled = this.statusService.configuration.features['web-cancel'];

  private checkpointIndexOfNavigation = this.checkpointIndexOfNav();
  private destroy$ = new Subject<void>();

  constructor(
    private readonly jobService: JobService,
    private readonly jobLocalService: JobLocalService,
    public readonly statusService: StatusService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  public ngOnInit(): void {
    const updateList$ = this.jobLocalService.jobDetailChanges().pipe(tap(data => this.handleJobDetailChanged(data)));
    const updateTip$ = this.jobLocalService.jobDetailChanges().pipe(
      distinctUntilKeyChanged('state'),
      tap(() => {
        this.statusTips = '';
        this.cdr.markForCheck();
      })
    );

    merge(updateList$, updateTip$).pipe(takeUntil(this.destroy$)).subscribe();
  }

  public ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  public cancelJob(): void {
    this.jobService.cancelJob(this.jobDetail.jid).subscribe(() => {
      this.statusTips = 'Cancelling...';
      this.cdr.markForCheck();
    });
  }

  public checkpointIndexOfNav(): number {
    return this.listOfNavigation.findIndex(item => item.path === 'checkpoints');
  }

  private handleJobDetailChanged(data: JobDetailCorrect): void {
    this.jobDetail = data;
    const index = this.checkpointIndexOfNav();
    if (data.plan.type == 'STREAMING' && index == -1) {
      this.listOfNavigation.splice(this.checkpointIndexOfNavigation, 0, {
        path: 'checkpoints',
        title: 'Checkpoints'
      });
    } else if (data.plan.type == 'BATCH' && index > -1) {
      this.listOfNavigation.splice(index, 1);
    }
    this.cdr.markForCheck();
  }
}
