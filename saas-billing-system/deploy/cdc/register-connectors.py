#!/usr/bin/env python3

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


CONNECT_URL = os.environ.get("CONNECT_URL", "http://kafka-connect:8083").rstrip("/")
CONNECTORS_DIR = Path(os.environ.get("CONNECTORS_DIR", "/etc/kafka-connect/connectors"))
READY_TIMEOUT_SECONDS = int(os.environ.get("CONNECT_READY_TIMEOUT_SECONDS", "60"))


def http_request(method: str, url: str, payload: object | None = None) -> tuple[int, str]:
  request = urllib.request.Request(url, method=method)
  request.add_header("Accept", "application/json")

  data = None
  if payload is not None:
    data = json.dumps(payload).encode("utf-8")
    request.add_header("Content-Type", "application/json")

  try:
    with urllib.request.urlopen(request, data=data) as response:
      return response.getcode(), response.read().decode("utf-8")
  except urllib.error.HTTPError as error:
    return error.code, error.read().decode("utf-8")


def wait_for_connect_ready() -> None:
  deadline = time.time() + READY_TIMEOUT_SECONDS
  url = f"{CONNECT_URL}/connector-plugins"

  while time.time() < deadline:
    status, _ = http_request("GET", url)
    if status == 200:
      return
    time.sleep(2)

  raise RuntimeError(f"Kafka Connect at {CONNECT_URL} did not become ready within {READY_TIMEOUT_SECONDS} seconds")


def register_connector(connector_file: Path) -> None:
  with connector_file.open("r", encoding="utf-8") as handle:
    connector_definition = json.load(handle)

  connector_name = connector_definition["name"]
  connector_config = connector_definition["config"]

  connector_url = f"{CONNECT_URL}/connectors/{connector_name}"
  config_url = f"{connector_url}/config"

  status, _ = http_request("GET", connector_url)
  if status == 200:
    update_status, body = http_request("PUT", config_url, connector_config)
    if update_status != 200:
      raise RuntimeError(f"Failed to update connector {connector_name}: HTTP {update_status} {body}")
    print(f"Updated connector {connector_name}")
    return

  create_status, body = http_request("POST", f"{CONNECT_URL}/connectors", connector_definition)
  if create_status not in (200, 201):
    raise RuntimeError(f"Failed to create connector {connector_name}: HTTP {create_status} {body}")
  print(f"Created connector {connector_name}")


def main() -> int:
  wait_for_connect_ready()

  connector_files = sorted(CONNECTORS_DIR.glob("*.json"))
  if not connector_files:
    print(f"No connector definitions found in {CONNECTORS_DIR}")
    return 0

  for connector_file in connector_files:
    register_connector(connector_file)

  return 0


if __name__ == "__main__":
  try:
    raise SystemExit(main())
  except Exception as error:
    print(str(error), file=sys.stderr)
    raise SystemExit(1)
