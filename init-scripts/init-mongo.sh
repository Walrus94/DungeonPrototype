#!/bin/bash
echo "Initializing MongoDB with environment variables..."

# Run MongoDB commands using the environment variables
mongo --host mongo --authenticationDatabase admin <<EOF
db = db.getSiblingDB("admin");
db.createUser({
  user: "${MONGO_MONITORING_USER}",
  pwd: "${MONGO_MONITORING_PASSWORD}",
  roles: [
          { role: "clusterMonitor", db: "admin" },
          { role: "read", db: "local" },
          { role: "read", db: "admin" }
      ]
});
EOF
