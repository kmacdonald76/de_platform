FROM debian:bookworm

RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

RUN curl https://dl.min.io/client/mc/release/linux-amd64/mc -o mc && install mc /usr/local/bin/mc && rm mc

RUN mkdir -p ~/.mc/

COPY mc.config.json ~/.mc/config.json

ENTRYPOINT ["/bin/sh", "-c"]
