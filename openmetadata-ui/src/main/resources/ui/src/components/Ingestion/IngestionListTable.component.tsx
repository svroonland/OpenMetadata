/*
 *  Copyright 2023 Collate.
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

import { Table, Tooltip, Typography } from 'antd';
import { ColumnsType } from 'antd/lib/table';
import NextPrevious from 'components/common/next-previous/NextPrevious';
import Loader from 'components/Loader/Loader';
import cronstrue from 'cronstrue';
import { Paging } from 'generated/type/paging';
import { isNil } from 'lodash';
import React, { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getEntityName } from 'utils/EntityUtils';
import { getErrorPlaceHolder } from 'utils/IngestionUtils';
import { PAGE_SIZE } from '../../constants/constants';
import { IngestionPipeline } from '../../generated/entity/services/ingestionPipelines/ingestionPipeline';
import { IngestionListTableProps } from './IngestionListTable.interface';
import { IngestionRecentRuns } from './IngestionRecentRun/IngestionRecentRuns.component';
import PipelineActions from './PipelineActions.component';

function IngestionListTable({
  airflowEndpoint,
  triggerIngestion,
  deployIngestion,
  isRequiredDetailsAvailable,
  paging,
  handleEnableDisableIngestion,
  onIngestionWorkflowsUpdate,
  ingestionPipelinesPermission,
  serviceCategory,
  serviceName,
  handleDeleteSelection,
  handleIsConfirmationModalOpen,
  ingestionData,
  deleteSelection,
  permissions,
  pipelineType,
  isLoading = false,
}: IngestionListTableProps) {
  const { t } = useTranslation();
  const [ingestionCurrentPage, setIngestionCurrentPage] = useState(1);

  const ingestionPagingHandler = (
    cursorType: string | number,
    activePage?: number
  ) => {
    const pagingString = `&${cursorType}=${paging[cursorType as keyof Paging]}`;

    onIngestionWorkflowsUpdate(pagingString);
    setIngestionCurrentPage(activePage ?? 1);
  };

  const renderNameField = (text: string, record: IngestionPipeline) => {
    return airflowEndpoint ? (
      <Tooltip
        title={
          permissions.ViewAll || permissions.ViewBasic
            ? t('label.view-entity', {
                entity: t('label.dag'),
              })
            : t('message.no-permission-to-view')
        }>
        <Typography.Link
          className="tw-mr-2 overflow-wrap-anywhere"
          data-testid="ingestion-dag-link"
          disabled={!(permissions.ViewAll || permissions.ViewBasic)}
          href={`${airflowEndpoint}/tree?dag_id=${text}`}
          rel="noopener noreferrer"
          target="_blank">
          {getEntityName(record)}
        </Typography.Link>
      </Tooltip>
    ) : (
      getEntityName(record)
    );
  };

  const renderScheduleField = (_: string, record: IngestionPipeline) => {
    return record.airflowConfig?.scheduleInterval ? (
      <Tooltip
        placement="bottom"
        title={cronstrue.toString(record.airflowConfig.scheduleInterval, {
          use24HourTimeFormat: true,
          verbose: true,
        })}>
        {record.airflowConfig.scheduleInterval}
      </Tooltip>
    ) : (
      <span>--</span>
    );
  };

  const renderActionsField = (_: string, record: IngestionPipeline) => {
    return (
      <PipelineActions
        deleteSelection={deleteSelection}
        deployIngestion={deployIngestion}
        handleDeleteSelection={handleDeleteSelection}
        handleEnableDisableIngestion={handleEnableDisableIngestion}
        handleIsConfirmationModalOpen={handleIsConfirmationModalOpen}
        ingestionPipelinesPermission={ingestionPipelinesPermission}
        isRequiredDetailsAvailable={isRequiredDetailsAvailable}
        record={record}
        serviceCategory={serviceCategory}
        serviceName={serviceName}
        triggerIngestion={triggerIngestion}
        onIngestionWorkflowsUpdate={onIngestionWorkflowsUpdate}
      />
    );
  };

  const tableColumn: ColumnsType<IngestionPipeline> = useMemo(
    () => [
      {
        title: t('label.name'),
        dataIndex: 'name',
        key: 'name',
        width: 500,
        render: renderNameField,
      },
      {
        title: t('label.type'),
        dataIndex: 'pipelineType',
        key: 'pipelineType',
      },
      {
        title: t('label.schedule'),
        dataIndex: 'schedule',
        key: 'schedule',
        render: renderScheduleField,
      },
      {
        title: t('label.recent-run-plural'),
        dataIndex: 'recentRuns',
        key: 'recentRuns',
        width: 180,
        render: (_, record) => (
          <IngestionRecentRuns classNames="align-middle" ingestion={record} />
        ),
      },
      {
        title: t('label.action-plural'),
        dataIndex: 'actions',
        key: 'actions',
        render: renderActionsField,
      },
    ],
    [
      permissions,
      airflowEndpoint,
      deployIngestion,
      triggerIngestion,
      isRequiredDetailsAvailable,
      handleEnableDisableIngestion,
      ingestionPipelinesPermission,
      serviceName,
      deleteSelection,
      handleDeleteSelection,
      serviceCategory,
      handleIsConfirmationModalOpen,
      onIngestionWorkflowsUpdate,
      ingestionData,
    ]
  );

  const showNextPrevious = useMemo(
    () =>
      Boolean(!isNil(paging.after) || !isNil(paging.before)) &&
      paging.total > PAGE_SIZE,
    [paging]
  );

  return (
    <div className="tw-mb-6" data-testid="ingestion-table">
      <Table
        bordered
        columns={tableColumn}
        data-testid="ingestion-list-table"
        dataSource={ingestionData}
        loading={{
          spinning: isLoading,
          indicator: <Loader size="small" />,
        }}
        locale={{
          emptyText: getErrorPlaceHolder(
            isRequiredDetailsAvailable,
            ingestionData.length,
            pipelineType
          ),
        }}
        pagination={false}
        rowKey="name"
        size="small"
      />

      {showNextPrevious && (
        <NextPrevious
          currentPage={ingestionCurrentPage}
          pageSize={PAGE_SIZE}
          paging={paging}
          pagingHandler={ingestionPagingHandler}
          totalCount={paging.total}
        />
      )}
    </div>
  );
}

export default IngestionListTable;
