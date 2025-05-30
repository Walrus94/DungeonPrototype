db = db.getSiblingDB("admin");
db.createUser({
    user: "${MONGO_MONITORING_USER}",
    pwd: "${MONGO_MONITORING_PASSWORD}",
    roles: [
        {role: "clusterMonitor", db: "admin"},
        {role: "read", db: "local"},
        {role: "read", db: "admin"}
    ]
});
