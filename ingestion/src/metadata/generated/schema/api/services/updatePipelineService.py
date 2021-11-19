# generated by datamodel-codegen:
#   filename:  schema/api/services/updatePipelineService.json
#   timestamp: 2021-11-18T23:20:04+00:00

from __future__ import annotations

from typing import Optional

from pydantic import AnyUrl, BaseModel, Field

from ...type import schedule


class UpdatePipelineServiceEntityRequest(BaseModel):
    description: Optional[str] = Field(
        None, description='Description of Pipeline service entity.'
    )
    pipelineUrl: Optional[AnyUrl] = Field(None, description='Pipeline Service UI URL.')
    ingestionSchedule: Optional[schedule.Schedule] = Field(
        None, description='Schedule for running metadata ingestion jobs'
    )
