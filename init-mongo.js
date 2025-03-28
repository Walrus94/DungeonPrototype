db = db.getSiblingDB("admin");
db.createUser({
    user: "monitoring",
    pwd: "monitoringpassword",
    roles: [
        { role: "clusterMonitor", db: "admin" },
        { role: "read", db: "local" }
    ]
});
