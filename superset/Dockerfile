FROM apache/superset:4.1.1
USER root
RUN pip install psycopg2-binary trino trino[sqlalchemy]
USER superset
