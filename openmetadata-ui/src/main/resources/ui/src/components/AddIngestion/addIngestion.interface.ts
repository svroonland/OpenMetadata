/*
 *  Copyright 2022 Collate.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import { DbtPipeline } from 'generated/metadataIngestion/dbtPipeline';
import { LoadingState, ServicesUpdateRequest } from 'Models';
import { FilterPatternEnum } from '../../enums/filterPattern.enum';
import { FormSubmitType } from '../../enums/form.enum';
import { ServiceCategory } from '../../enums/service.enum';
import {
  CreateIngestionPipeline,
  DBTConfigurationSource,
  Pipeline,
} from '../../generated/api/services/ingestionPipelines/createIngestionPipeline';
import { ProfileSampleType } from '../../generated/entity/data/table';
import {
  FilterPattern,
  IngestionPipeline,
  PipelineType,
} from '../../generated/entity/services/ingestionPipelines/ingestionPipeline';
import {
  DBT_SOURCES,
  GCS_CONFIG,
} from '../common/DBTConfigFormBuilder/DBTFormEnum';

export interface AddIngestionProps {
  activeIngestionStep: number;
  pipelineType: PipelineType;
  heading: string;
  ingestionAction?: string;
  status: FormSubmitType;
  data?: IngestionPipeline;
  serviceCategory: ServiceCategory;
  serviceData: ServicesUpdateRequest;
  showSuccessScreen?: boolean;
  showDeployButton?: boolean;
  setActiveIngestionStep: (step: number) => void;
  handleCancelClick: () => void;
  onAddIngestionSave?: (ingestion: CreateIngestionPipeline) => Promise<void>;
  onIngestionDeploy?: () => Promise<void>;
  onUpdateIngestion?: (
    data: IngestionPipeline,
    oldData: IngestionPipeline,
    id: string,
    displayName: string,
    triggerIngestion?: boolean
  ) => Promise<void>;
  onSuccessSave?: () => void;
  isIngestionDeployed?: boolean;
  isIngestionCreated?: boolean;
  ingestionProgress?: number;
  handleViewServiceClick?: () => void;
  onFocus: (fieldName: string) => void;
}

export interface ConfigureIngestionProps {
  data: AddIngestionState;
  formType: FormSubmitType;
  getExcludeValue: (value: string[], type: FilterPatternEnum) => void;
  getIncludeValue: (value: string[], type: FilterPatternEnum) => void;
  handleShowFilter: (value: boolean, type: string) => void;
  onCancel: () => void;
  onChange: (newState: Partial<AddIngestionState>) => void;
  onNext: () => void;
  pipelineType: PipelineType;
  serviceCategory: ServiceCategory;
  onFocus: (fieldName: string) => void;
}

export type ScheduleIntervalProps = {
  onChange: (newState: Partial<AddIngestionState>) => void;
  status: LoadingState;
  repeatFrequency: string;
  includePeriodOptions?: string[];
  submitButtonLabel: string;
  disabledCronChange?: boolean;
  onBack: () => void;
  onDeploy: () => void;
};

// Todo: Need to refactor below type, as per schema change #9575
export type ModifiedDBTConfigurationSource = DBTConfigurationSource &
  Pick<
    DbtPipeline,
    | 'dbtUpdateDescriptions'
    | 'dbtClassificationName'
    | 'includeTags'
    | 'databaseFilterPattern'
    | 'schemaFilterPattern'
    | 'tableFilterPattern'
  >;

export interface AddIngestionState {
  dataModelFilterPattern: FilterPattern;
  chartFilterPattern: FilterPattern;
  database?: string;
  dashboardFilterPattern: FilterPattern;
  databaseFilterPattern: FilterPattern;
  containerFilterPattern: FilterPattern;
  isDatabaseFilterDisabled: boolean;
  databaseServiceNames: string[];
  dbtClassificationName: string;
  dbtUpdateDescriptions: boolean;
  dbtConfigSource: ModifiedDBTConfigurationSource;
  dbtConfigSourceType: DBT_SOURCES;
  description: string;
  enableDebugLog: boolean;
  filterCondition: string;
  gcsConfigType: GCS_CONFIG | undefined;
  includeLineage: boolean;
  includeTags: boolean;
  includeView: boolean;
  includeDataModels: boolean;
  ingestionName: string;
  ingestSampleData: boolean;
  markAllDeletedTables: boolean | undefined;
  markDeletedTables: boolean | undefined;
  markDeletedDashboards?: boolean;
  markDeletedTopics?: boolean;
  markDeletedMlModels?: boolean;
  markDeletedPipelines?: boolean;
  metadataToESConfig: Pipeline | undefined;
  mlModelFilterPattern: FilterPattern;
  pipelineFilterPattern: FilterPattern;
  profileSample: number | undefined;
  profileSampleType: ProfileSampleType;
  queryLogDuration: number;
  repeatFrequency: string;
  resultLimit: number;
  saveState: LoadingState;
  schemaFilterPattern: FilterPattern;
  showDataModelFilter: boolean;
  showChartFilter: boolean;
  showDashboardFilter: boolean;
  showDatabaseFilter: boolean;
  showDeployModal: boolean;
  showMlModelFilter: boolean;
  showPipelineFilter: boolean;
  showSchemaFilter: boolean;
  showTableFilter: boolean;
  showTopicFilter: boolean;
  stageFileLocation: string;
  tableFilterPattern: FilterPattern;
  threadCount: number;
  timeoutSeconds: number;
  topicFilterPattern: FilterPattern;
  useFqnFilter: boolean;
  viewParsingTimeoutLimit: number;
  parsingTimeoutLimit: number;
  processPii: boolean;
  includeOwners: boolean;
  confidence?: number;
  showContainerFilter: boolean;
}

export enum ShowFilter {
  showChartFilter = 'showChartFilter',
  showDashboardFilter = 'showDashboardFilter',
  showDatabaseFilter = 'showDatabaseFilter',
  showMlModelFilter = 'showMlModelFilter',
  showPipelineFilter = 'showPipelineFilter',
  showSchemaFilter = 'showSchemaFilter',
  showTableFilter = 'showTableFilter',
  showTopicFilter = 'showTopicFilter',
  showContainerFilter = 'showContainerFilter',
  showDataModelFilter = 'showDataModelFilter',
}
