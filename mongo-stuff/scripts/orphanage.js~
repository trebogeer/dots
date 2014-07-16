// "gborphan"/ "gb4test" (userid/password)
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
  shardConnection: function(shard){:
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
    this.configDB().shards.find().forEach( function (shard) {
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
  find: function(namespace) {
    // Make sure this script is being run on mongos
    assert(Shard.configDB().runCommand({ isdbgrid: 1}).ok, "Not a sharded cluster")

    assert(!sh.getBalancerState(), "Balancer must be stopped first")
    assert(!sh.isBalancerRunning(), "Balancer is still running, wait for it to finish")

    print("Searching for orphans in namespace [" + namespace + "]")
    var shardConns = Shard.connections()
    var connections = {};

    var precise = 1;
    if (typeof bsonWoCompare === 'undefined') {
        print("bsonWoCompare is undefined. Orphaned document counts might be higher than the actual numbers");
        print("Try running with mongo shell >2.5.3");
        precise = 0;
    }

    // skip shards that have no data yet
    for(shard in shardConns) {
        if (shardConns[shard].getCollection(namespace).count() > 0)
            connections[shard] = shardConns[shard];
    }

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
      if (precise) {
        if (bsonWoCompare(result.maxRange, chunk.max) < 0) { // stored max is smaller, so we have not seen this chunk
          result.maxRange = chunk.max;
          result.lastMin = chunk.min;
          allChunks.push(chunk);
        } else {
          print("Skipping chunk (split?) with max " + chunk.max);
          assert(bsonWoCompare(result.lastMin, chunk.min) <= 0, "Chunk order is screwed!");
        }
      }
    });

    var wConn = Orphanage.workerConnection().getDB('admin');
      // query all non-authoritative shards
    var totalCnt = 0;
    var start = new Date().getTime();
    for (var shard in connections) {
      //if (shard != chunk.shard) {
          // make connection to non-authoritative shard
          var naCollection = connections[shard].getCollection(namespace)
          var orphanCollecion = connections[shard].getCollection(namespace + '_orphans')

          // gather documents that should not exist here
          //var orphanCount = 0;//naCollection.find()._addSpecial("$returnKey", true).min(chunk.min).max(chunk.max).itcount();

          naCollection.find().forEach(function(doc) {
              var hashedId = wConn.db.runCommand({_hashBSONElement: doc['_id']});
              if(hashedId['ok'] == 1 && hashedId['out']) {
              allChunks.forEach(function(chunk) {
                  if (shard != chunk.shard) {
                       var orphanCount = 0;
                       if (hashedId['out'] >= chunk.min['_id'] && hashedId['out'] < chunk.max['_id']) {
                          print('Found orphan doc - ' + doc['_id'] + ' on shard: ' + shard);
                          orphanCount = orphanCount + 1;
                          orphanCollection.update(doc, doc, {upsert:true});
                       }


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
             });
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
  findAll: function(){
    var result = {}
    var namespaces = Shard.namespaces()

    for (i in namespaces) {
      namespace = namespaces[i];
      result[namespace] = this.find(namespace);
    }
    return result;
  },
  // Remove all orphaned chunks
  removeAll: function(nsMap) {
      //var num = 0;
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

print("***                    Loaded orphanage_hashed.js                    ***")
print("")
