#!/bin/sh
set -euo pipefail

# Create provisioning directory for datasources
mkdir -p /etc/grafana/provisioning/datasources

# Write the datasource provisioning YAML
cat <<EOF > /etc/grafana/provisioning/datasources/ds.yaml
apiVersion: 1
datasources:
- name: Loki
  type: loki
  access: proxy
  orgId: 1
  url: http://loki:3100
  basicAuth: false
  isDefault: true
  version: 1
  editable: false
- name: Prometheus
  type: prometheus
  access: proxy
  orgId: 1
  url: http://prometheus:9090
  basicAuth: false
  isDefault: false
  version: 1
  editable: true
EOF

# Start the Grafana server
exec /run.sh
