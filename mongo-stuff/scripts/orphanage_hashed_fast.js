
var Orphanage = {
    globalAuthDoc: null,
    shardAuthDocs: {},
    global: {
        auth: (function(self){return function(user,pwd){
            self.Orphanage.globalAuthDoc = {'user':user,'pwd':pwd};
        }})(this)
    },
    shard: {
        auth: (function(self){return function(shard,user,pwd){
            self.Orphanage.shardAuthDocs[shard] = {'user':user,'pwd':pwd};
        }})(this)
    },
    copyDoc: function(doc){
        var newDoc = {};
        for (var prop in doc) {
            newDoc[prop] = doc[prop];
        }
        return newDoc;
    },
    shardConnection: function(shard){
        var conn = new Mongo(shard.host);
        var admin = conn.getDB("admin");

        // try shard specific auth first
        if (this.shardAuthDocs[shard._id]){
            // copy authDoc as we do not want auth
            // to modify the original SERVER-11626
            var authDoc = this.copyDoc(this.shardAuthDocs[shard._id]);

            // if that fails try global auth
            if (admin.auth(authDoc) != 1 && this.globalAuthDoc){
                authDoc = this.copyDoc(this.globalAuthDoc);
                admin.auth(authDoc);
            }
        } else if (this.globalAuthDoc){
            var authDoc = this.copyDoc(this.globalAuthDoc);
            admin.auth(authDoc);
        }
        return conn;
    },
    workerConnection:function() {
        var conn = new Mongo('localhost:27017');
        return conn;
    }
}

// Shard object -- contains shard related functions
var Shard = {
    tShard: null,
    target:{
        set:(function(self){return function(id){
            self.Shard.tShard = {'_id': id};
        }})(this)
    },
    configDB: function() {return db.getSiblingDB("config");},
    active: [],
    // Returns an array of sharded namespaces
    namespaces: function(){
        var nsl = [] // namespace list
            this.configDB().collections.find().forEach(function(ns){nsl.push(ns._id)})
            return nsl
    },

    // Returns map of shard names -> shard connections
    connections: function() {
        var conns = {}
        this.configDB().shards.find(this.tShard == null?{}:{_id: this.tShard['_id']}).forEach( function (shard) {
            // skip inactive shards (use can specify active shards)
            if (Shard.active && Shard.active.length > 0 && !Array.contains(Shard.active, shard._id))
            return;
        conns[shard._id] = Orphanage.shardConnection(shard);
        });
        return conns;
    }
}

// Orphans object -- finds and removes orphaned documents
var Orphans = {
    connections: function(namespace) {

        var shardConns = Shard.connections();
        var connections = {};

        for(shard in shardConns) {
            if (shardConns[shard].getCollection(namespace).count() > 0)
                connections[shard] = shardConns[shard];
        }
        return connections;
    },
    find: function(namespace) {


        assert(Shard.configDB().runCommand({ isdbgrid: 1}).ok, "Not a sharded cluster")

            assert(!sh.getBalancerState(), "Balancer must be stopped first")
            assert(!sh.isBalancerRunning(), "Balancer is still running, wait for it to finish")


            var precise = 1;

        if (typeof bsonWoCompare === 'undefined') {
            print("bsonWoCompare is undefined. Orphaned document counts might be higher than the actual numbers");
            print("Try running with mongo shell >2.5.3");
            precise = 0;
        }


        print("Searching for orphans in namespace [" + namespace + "]")
            var connections = this.connections(namespace);
        //int(connections)

        var result = {
            parent: this,
            badChunks: {},
            maxRange: {},
            lastMin: {},
            count: 0,
            shardCounts:{}
        }

        var allChunks = [];
        // iterate over chunks -- only one shard should own each chunk
        Shard.configDB().chunks.find({ ns: namespace }).sort({min : 1}).batchSize(5).forEach( function(chunk) {
            // check if we already seen this chunk

            if(chunk['_id'].indexOf("MinKey") > -1) {
                chunk.min._id = Number.MIN_VALUE;
            }
            if(chunk['_id'].indexOf("MaxKey") > -1) {
                chunk.max._id = Number.MAX_VALUE;
            }
            if (precise) {
                if (bsonWoCompare(result.maxRange, chunk.max) < 0) { // stored max is smaller, so we have not seen this chunk
                    result.maxRange = chunk.max;
                    result.lastMin = chunk.min;
                    allChunks.push(chunk);
                } else {
                    print("Skipping chunk (split?) with max " + chunk.max);
                    assert(bsonWoCompare(result.lastMin, chunk.min) <= 0, "Chunk order is screwed!");
                }
            } else {
                allChunks.push(chunk);
            }
        });
        print("Chunks found: " + allChunks.length);
        var wConn = Orphanage.workerConnection().getDB('admin');
        // query all non-authoritative shards
        var totalCnt = 0;
        var start = new Date().getTime();
        for (var shard in connections) {
            // make connection to non-authoritative shard
            var naCollection = connections[shard].getCollection(namespace)
                var orphanCollection = connections[shard].getCollection(namespace + '_orphans')

                naCollection.find().forEach(function(doc) {
                    var hashedId = wConn.db.runCommand({_hashBSONElement: doc['_id']});
                    if(hashedId['ok'] == 1) {
                        var t = new Date().getTime();
                        var hashed_id = hashedId['out'];
                        allChunks.forEach(function(chunk) {
                            if (shard != chunk.shard) {
                                var orphanCount = 0;
                                //                       if(precise == 1) {
                                //                          if ((bsonWoCompare({x:hashedId['out']}, {x:chunk.min['_id']}) >= 0) && (bsonWoCompare({x:hashedId['out']}, {x:chunk.max['_id']}) < 0)) {
                                //                             print('Found orphan doc - ' + doc['_id'] + ' on shard: ' + shard);
                                //                             orphanCount = orphanCount + 1;
                                //                             orphanCollection.update(doc, doc, {upsert:true});
                                //                          }
                                //                       } else {
                                if (hashed_id >= chunk.min['_id'] && hashed_id < chunk.max['_id']) {
                                    print('Found orphan doc - ' + doc['_id'] + ' on shard: ' + shard);
                                    orphanCount = orphanCount + 1;
                                    orphanCollection.update(doc, doc, {upsert:true});

                                    if (orphanCount > 0) {
                                        result.count += orphanCount

                                    // keep count by shard
                                    if(!result.shardCounts[shard])
                                       result.shardCounts[shard] = orphanCount;
                                    else
                                       result.shardCounts[shard] += orphanCount;

                                    chunk.orphanedOn = shard
                                    chunk.orphanCount = orphanCount
                                    if (!result.badChunks[chunk['_id']]) {
                                        result.badChunks[chunk['_id']] = chunk
                                    }
                                    }
                                }
                                //                       }

                            }
                        });
                        //print("Time to analyze one document: " + (new Date().getTime() - t));
                    } else {
                        print("Failed to compute hash. Skipping doc with id: " + doc['_id']);
                    }
                    totalCnt += 1;
                    if (totalCnt%10000 == 0) {
                        print("Analyzed " + totalCnt + " documents. Elapsed time: " + ((new Date().getTime() - start) / 1000) + " sec.");
                    }
                });
        }
        print("Analyzed " + totalCnt + "documents total. Elapsed time : " + ((new Date().getTime() - start)/1000) + " sec.");

        if (result.count > 0) {
            print("-> " + result.count + " orphan(s) found in " + result.badChunks.length +
                  " chunks(s) in namespace [" + namespace + "]\n\tOrphans by Shard:")
                print("\t\t" + tojson(result.shardCounts));
            print("");
        } else {
            print("-> No orphans found in [" + namespace  + "]\n")
        }
        return result
    },
    //print stats on individual shards
    stats: function(namespace) {

        var connections = this.connections(namespace);


        for (var shard in connections) {
            print("***                       shard: " + shard + "                    *** ")
                var orphanCollection = connections[shard].getCollection(namespace + '_orphans')
                printjson(orphanCollection.stats())
                print("======================================================================")
        }
    },
    // Remove all orphaned chunks
    remove: function(namespace) {

        var connections = this.connections(namespace);

        for (var shard in connections) {
            print ("**** Removing orphan documents from shard (if any): " + shard);
            var orphanCollection = connections[shard].getCollection(namespace + '_orphans')
                if(orphanCollection.count() > 0){
                    var cnt = 0;
                    var naCollection = connections[shard].getCollection(namespace);
                    orphanCollection.find({},{_id:1}).forEach(function(doc) {naCollection.remove({_id:doc['_id']}, true);cnt += 1})
                        print("***  Removed " + cnt + " documents");
                }

        }
        //if(nsMap)
        //    for(ns in nsMap)
        //        num += nsMap[ns].removeAll();

        //return num;
    },
    // Balancer paranoia is on by default
    _balancerParanoia: true,
    setBalancerParanoia: function(b) {
        this._balancerParanoia = b;
    }
}

print("***                 Loaded orphanage_hashed_fast.js                  ***")
print("***                                                                  ***")
print("***                              Usage:                              ***")
print("")
print("1. Use mongo shell 2.6  - mongo --nodb")
print("2. Run")
print(" mongod --smallfiles --dbpath /tmp --logpath /tmp/log --nojournal --setParameter enableTestCommands=1")
print("on the same machine as mongo shell")
print("3. Orphanage.global.auth('username', 'password')")
print("4. Orphans.find('db.collection')")
print("5. Orphans.stats('db.collection')")
print("6. Orphans.remove('db.collection')")
print("")
