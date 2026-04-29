#!/bin/bash

set -euo pipefail

TOPICS_FILE="${TOPICS_FILE:-/etc/kafka/topics.yaml}"
BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-kafka:9092}"

cub kafka-ready -b "${BOOTSTRAP_SERVER}" 1 60

awk '
  function flush() {
    if (name == "") {
      return
    }
    print name "\t" partitions "\t" replication "\t" configs
    configs = ""
    in_configs = 0
  }
  /^  - name:/ {
    flush()
    name = $3
    next
  }
  /^    partitions:/ {
    partitions = $2
    next
  }
  /^    replicationFactor:/ {
    replication = $2
    next
  }
  /^    configs:$/ {
    in_configs = 1
    next
  }
  in_configs && /^      / {
    line = $0
    sub(/^      /, "", line)
    split(line, parts, /: /)
    if (configs != "") {
      configs = configs ","
    }
    configs = configs parts[1] "=" parts[2]
    next
  }
  in_configs && !/^      / {
    in_configs = 0
  }
  END {
    flush()
  }
' "${TOPICS_FILE}" | while IFS=$'\t' read -r name partitions replication configs; do
  topic_args=(
    --bootstrap-server "${BOOTSTRAP_SERVER}"
    --create
    --if-not-exists
    --topic "${name}"
    --partitions "${partitions}"
    --replication-factor "${replication}"
  )

  if [[ -n "${configs}" ]]; then
    IFS=',' read -ra topic_configs <<< "${configs}"
    for topic_config in "${topic_configs[@]}"; do
      topic_args+=(--config "${topic_config}")
    done
  fi

  kafka-topics \
    "${topic_args[@]}"
done
