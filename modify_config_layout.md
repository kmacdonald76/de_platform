## Context

Please read the following two spec sheets for context:

secrets/spec_sheet.md
flink/apps/spec_sheet.md

## Instructions

Update the bronze layer configuration layout so the source section has type-specific configurations put under a sub-section with the type as the title. 

For example, 

source:
    type: api
    format: json
    packaging: none
    connection:
        url: https://formulae.brew.sh/api/analytics/cask-install/30d.json

connection.url is specific to the api type, and should instead go under an api section, like so:


source:
  type: api
  format: json
  packaging: none
  auth:
    method: none
  api:
    url: https://formulae.brew.sh/api/analytics/cask-install/30d.json


We need to update both sub-projects: 

 - the layout of the existing secrets/*/bronze/*.yaml files need to be structured as described above.
 - the flink ingestion code needs to be modified to be able to read and use the new configuration layout

Do not commit changes to the secrets repo, the files need to be encrypted first, which I will do manually after your changes are made.
