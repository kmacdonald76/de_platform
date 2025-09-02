FROM apache/airflow:3.0.4-python3.12

COPY requirements.txt /

# Define ARGs for flexibility
ARG AIRFLOW_VERSION=3.0.4
ARG PYTHON_VERSION=3.12

# Fetch and use the correct constraints file
RUN pip install --no-cache-dir -r /requirements.txt -c ~/constraints.txt
