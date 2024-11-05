To access the container and run influx db commnds locally

After docker compose available in http://localhost:8086/


To access client shell
```
docker exec -it influxdb bash

influx v1 shell --org-id {org-id} --token {token}
```

{org-id} is from:

    open http://localhost:8086/
    open menu profile > about > Organization ID

{token} is from:

    http://localhost:8086/api/v2/authorizations
