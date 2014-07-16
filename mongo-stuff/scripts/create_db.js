
// mongo --host greendb301p.dev.ch3.s.com -ugreenadmin -pqAD9wJBB --port 20000 admin ~/green-box/mongo/scripts/create_db.js


function log(m) {
    var date = new Date(),
        timestamp = date.toLocaleDateString() + " " + date.toLocaleTimeString();

    print(timestamp + ": " + m);
}

function isDbSharded(configDB, dbName) {
	return configDB.databases.find({_id: dbName, partitioned: true}).hasNext();
}

sh.stopBalancer();

if (!sh.isBalancerRunning()) {

dbs = [
    "test"
];

//dbs = ["uvdsku"];
adminDB = db.getSiblingDB("admin");
var stats = {};
var configDB = db.getSisterDB("config");
for (var i = 0; i < dbs.length; i++) {
    dbName = dbs[i];
    log("[INFO] Creating database: '" + dbName + "'.");
    db_ = db.getSiblingDB(dbName);
    if (!isDbSharded(configDB, dbName)) {
    	adminDB.runCommand({"enableSharding": dbName});
    	log("[INFO] Sharding enabled: '" + dbName + "'.");
    } else {
    	log("[INFO] Database " + dbName + " already sharded");
    }
    	coll = db_.getCollection(dbName);
    	coll.ensureIndex({"_bucket": 1, "_tmstmp": 1, "_id": 1});
    	log("[INFO] Created collection: '" + coll.getFullName() + "'.");
    	var collStats = coll.stats();
    	if (!collStats.sharded) {
    		adminDB.runCommand({"shardCollection": coll.getFullName(), key: {"_id": "hashed"}});
    		log("[INFO] Sharded collection: '" + coll.getFullName() + "'.");
    	} else {
    		log("[INFO] Collection" + dbName + " already sharded");
    	}
        stats[db_ + "." + dbName] = collStats;

        coll = db_.getCollection(dbName + "_del");
        coll.ensureIndex({"_bucket": 1, "_tmstmp": 1, "_id": 1});
        coll.ensureIndex({"_tmstmp": 1}, {"expireAfterSeconds": 6 * 30 * 24 * 60 * 60});
        log("[INFO] Created collection: '" + coll.getFullName() + "'.");
        var collStats = coll.stats();
        if (!collStats.sharded) {
        	adminDB.runCommand({"shardCollection": coll.getFullName(), "key": {"_id": "hashed"}});
        	log("[INFO] Sharded collection: '" + coll.getFullName() + "'.");
        } else {
        	log("[INFO] Collection" + dbName + "_del already sharded");
        }
        stats[db_ + "." + dbName + "_del"] = coll.stats();

        coll = db_.getCollection(dbName + "_snapshot");
        coll.ensureIndex({"_sid": 1,"_tmstmp": 1}, { unique: true });
        log("[INFO] Created collection: '" + coll.getFullName() + "'.");
        var collStats = coll.stats();
        if (!collStats.sharded) {
            adminDB.runCommand({"shardCollection": coll.getFullName(), "key": {"_sid": 1,"_tmstmp": 1}});
            log("[INFO] Sharded collection: '" + coll.getFullName() + "'.");
        } else {
            log("[INFO] Collection" + dbName + "_snapshot already sharded");
        }
        stats[db_ + "." + dbName + "_snapshot"] = coll.stats();
}
log("[INFO] Summary: ");
for (var field in stats) {
    if(stats[field]){
	log("[INFO] Collection" + field + " Summary: ");
	printjson(stats[field]);
	}
}
log("[INFO] Sharding Status: ");
printjson(sh.status());
sh.startBalancer();
if(!sh.isBalancerRunning()) {
    log("[ERROR] Failed to start balancer after script execution.");
}
} else {
log("[ERRO] Failed to stop balancer. Will not execute any scripts.");
}


/**sh.*/
