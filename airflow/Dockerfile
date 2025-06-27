FROM apache/airflow:2.10.5-python3.12

COPY requirements.txt /

# Define ARGs for flexibility
ARG AIRFLOW_VERSION=2.10.5
ARG PYTHON_VERSION=3.12

# Fetch and use the correct constraints file
RUN pip install --no-cache-dir -r /requirements.txt -c ~/constraints.txt
