

db.getSiblingDB('content').lost_and_found.find({},{'_id':1}).forEach(function(doc){print(doc['_id']);});
