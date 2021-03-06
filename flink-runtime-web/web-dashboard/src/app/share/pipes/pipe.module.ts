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

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { HumanizeBytesPipe } from '@flink-runtime-web/share/pipes/humanize-bytes.pipe';
import { HumanizeWatermarkPipe } from '@flink-runtime-web/share/pipes/humanize-watermark.pipe';

import { HumanizeChartNumericPipe } from './humanize-chart-numeric.pipe';
import { HumanizeDatePipe } from './humanize-date.pipe';
import { HumanizeDurationPipe } from './humanize-duration.pipe';
import { ParseIntPipe } from './parse-int.pipe';

@NgModule({
  imports: [CommonModule],
  declarations: [
    HumanizeDurationPipe,
    HumanizeDatePipe,
    HumanizeBytesPipe,
    HumanizeWatermarkPipe,
    ParseIntPipe,
    HumanizeChartNumericPipe
  ],
  exports: [
    HumanizeDurationPipe,
    HumanizeDatePipe,
    HumanizeBytesPipe,
    HumanizeWatermarkPipe,
    HumanizeChartNumericPipe,
    ParseIntPipe
  ]
})
export class PipeModule {}
