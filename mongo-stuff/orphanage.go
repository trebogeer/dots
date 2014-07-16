package main

import (
    "fmt"
    "os"
    "flag"
    "regexp"
    "strings"
    "strconv"
    "time"
    "sync"
    "math"
    "labix.org/v2/mgo"
    "labix.org/v2/mgo/bson"
)


type Shard struct {
    ID string `bson:"_id"`
    Host string `bson:"host"`
}

type Chunk struct {
    ID string `bson:"_id"`
    Shard string `bson:"shard"`
    Min Entry `bson:"min"`
    Max Entry `bson:"max"`
}

type Entry struct {
  Val int64 `bson:"_id"`
}

type Id struct {
    ID string `bson:"_id"`
}

func main() {

    host := flag.String("host", "greenapi307p.stress.ch3.s.com", "MongoS host to connect to.")
    port := flag.Int("port", 20000, "MongoS port to connect to.")
    user := flag.String("u","gborphan", "MongoDB username.")
    pwd := flag.String("p", "gb4test", "MongoDB password.")
    audb := flag.String("authdb", "admin", "MongoDB database to authenticate against.")
    dbName := flag.String("db", "offer", "MongoDB database to clean up orphan documents.")
    collName := flag.String("c", "offer", "MongoDB collection to cleanup orphan documents.")
    remove := flag.String("r", "no", "Flag to remove orphan documnts from target collection")

    flag.Parse()

    mdbDialInfo := &mgo.DialInfo {
        Addrs: []string{*host + ":"+ strconv.Itoa(*port)},
        Source: *audb,
        Username: *user,
        Password: *pwd,
    }

    fmt.Println("MongoS Host: " + *host)
    p := fmt.Sprintf("MongoDB Port: %v", *port)
    fmt.Println(p)
    fmt.Println("MongoS User: " + *user)
    reg, _:= regexp.Compile(".*")
    fmt.Println("MongoDB Password: " + reg.ReplaceAllString(*pwd, "*"))
    fmt.Println("MongoDB Auth Database: " + *audb)
    fmt.Println("MongoDB Database: " + *dbName)
    fmt.Println("MongoDB Collection: " + *collName)
    fmt.Println("Remove orphans: " + *remove)
    session, err := mgo.DialWithInfo(mdbDialInfo)
    if err != nil {
	   panic(err)
    }
    defer session.Close()
    session.SetMode(mgo.Monotonic, true)
    session.EnsureSafe(&mgo.Safe{W:0, FSync:false})



    //preconditions check
    is_cluster := bson.M{}
    err = session.DB("config").Run(bson.M{"isdbgrid":1}, &is_cluster)

    if err != nil {
       panic(err)
    }

    if is_cluster["ok"] == nil || is_cluster["ok"].(float64) != 1 {
       fmt.Println("Not a sharded cluster. Exiting now.")
       os.Exit(1)
    }


    config_s := session.DB("config").C("settings")
    balancer := bson.M{}
    err = config_s.Find(bson.M{"_id":"balancer"}).One(&balancer)
    if err !=  nil {
       panic(err)
    }

    if balancer["stopped"] == nil || !balancer["stopped"].(bool) {
      fmt.Println("Balancer must be stopped first. Exiting now.")
      os.Exit(1)
    }

    locks_s := session.DB("config").C("locks")
    b_lock := bson.M{}

    err = locks_s.Find(bson.M{"_id":"balancer"}).One(&b_lock)
    if err != nil {
      panic(err)
    }

    if b_lock["state"] != nil && b_lock["state"].(int) > 0 {
       fmt.Println("Balancer is still running, wait for it to finish. Exiting now." )
       os.Exit(1)
    }

    // obtaining shard information
    shards_c := session.DB("config").C("shards")
    var shards []Shard
    err = shards_c.Find(bson.M{}).All(&shards)
    if err != nil {
        fmt.Println("Failed to get shards from config database.")
        panic(err)
    }

    if len(shards) < 1 {
       fmt.Println("This is not a sharded mongo cluster. Nothing to do here. Exiting now.")
       os.Exit(1)
    } else {
       fmt.Println("Found " + strconv.Itoa(len(shards)) + " shards.")
    }

    if *remove != "no" {
       var waitGroup sync.WaitGroup
       waitGroup.Add(len(shards))

       for i:= range shards {
         go removeOrphans(&waitGroup, *dbName, *collName, *user, *pwd, &shards[i])
       }
       waitGroup.Wait()
    } else {

    // obtaining chunk information
    ns := *dbName + "." + *collName
    var chunks []Chunk
    chunks_c := session.DB("config").C("chunks")
    err = chunks_c.Find(bson.M{"ns":ns}).Sort("min").All(&chunks)
    if err != nil {
       fmt.Println("Failed to get chunks from config database.")
       panic(err)
    }

    if len(chunks) < 1 {
       fmt.Println("No chunks found for the specified namespace : " + ". Exiting now.")
    } else {
       fmt.Println("Found " + strconv.Itoa(len(chunks)) + " chunks in namespace: " + ns)
    }

    for i:= range chunks {
      if strings.Index(chunks[i].ID, "MinKey") > -1 {
          chunks[i].Min.Val = math.MinInt64
          fmt.Printf("MinKey lower bound : %v\n", chunks[i].Min.Val)
      }

      if strings.Index(chunks[i].ID, "MaxKey") > -1 {
          chunks[i].Max.Val = math.MaxInt64
          fmt.Printf("MaxKey upper bound : %v\n", chunks[i].Max.Val)
      }

      fmt.Printf("chunk_id: %s, chunk_shard: %v, min: %v, max: %v\n" , chunks[i].ID, chunks[i].Shard, chunks[i].Min.Val, chunks[i].Max.Val)
    }

    var waitGroup sync.WaitGroup
    waitGroup.Add(len(shards))
    for i := 0; i < len(shards); i++ {
      go findOrphans(&waitGroup, *dbName, *collName, *user, *pwd, &shards[i], chunks)
    }
    waitGroup.Wait()
    }

    fmt.Println("Bye!")
}

func findOrphans(waitGroup *sync.WaitGroup ,db, c, u, p string, shard *Shard, chunks []Chunk) {


    fmt.Println("Starting routine for shard: [" + shard.Host + "]")
    defer waitGroup.Done()

    hosts := strings.Split(shard.Host[strings.Index(shard.Host, "/") + 1:len(shard.Host)], ",")

    dialInfo := &mgo.DialInfo {
        Addrs: hosts,
        Source: "admin",
        Username: u,
        Password: p,
    }

    testEnabledSession, err := mgo.Dial("localhost:27017")
    if err != nil {
      panic(err)
    }
    defer testEnabledSession.Close()
    fmt.Println("[" + shard.Host + "] Connected to test db.")
    session, err := mgo.DialWithInfo(dialInfo)
    if err != nil {
	   panic(err)
    }
    defer session.Close()
    session.SetMode(mgo.Monotonic, true)
    session.EnsureSafe(&mgo.Safe{W:0, FSync:false})

    fmt.Println("[" + shard.Host + "] Connected to shard.")

    hasher := testEnabledSession.DB("test")
    source := session.DB(db).C(c)
    target := session.DB(db).C(c + "_orphans")

    tq := source.Find(bson.M{}).Select(bson.M{"_id":1})
    it := tq.Iter()
    cnt := 0
    ch_l := len(chunks)
    start := time.Now().UnixNano()
    for {
      var id_ Id
      if !it.Next(&id_) {
        break
      } else {
        id := id_.ID
        cnt++
        hashed := bson.M{}
        err = hasher.Run(bson.M{"_hashBSONElement": id}, &hashed)
        if err != nil {
           fmt.Println("Failed to hash id " + id)
        } /*else {
           fmt.Println()
           fmt.Printf("Hash %v", hashed["out"])
        }*/
        hashed_id := hashed["out"].(int64)
        for i := 0; i < ch_l; i++ {
          if shard.ID != chunks[i].Shard && hashed_id >= chunks[i].Min.Val && hashed_id < chunks[i].Max.Val {
            fmt.Printf("Found orphan document with id %s on shard[%s], chunk_id: %s, chunk_shard: %v, min: %v, max: %v\n" , id, shard.Host, chunks[i].ID, chunks[i].Shard, chunks[i].Min.Val, chunks[i].Max.Val)
            doc := bson.M{}
            err = source.FindId(id).One(&doc)
            if err != nil {
              fmt.Printf("Failed to retrieve document from '%s.%s' with _id %v\n", db, c, id)
              fmt.Println(err)
            } else {
              _, err = target.Upsert(doc, doc)
              if err != nil {
                  fmt.Printf("Failed to insert doc with id '%v' into target namespace '%s.%s_orphange'.\n", id, db,c)
              }
            }
          }
        }
        if cnt%100000 == 0 {
           t := time.Now().UnixNano()
           r := (t - start)/1000000000
           fmt.Println("[" + shard.Host + "] Analyzed " + strconv.Itoa(cnt) + " documents. Elapsed time: " + strconv.FormatInt(r, 10) + " sec")
        }

      }
    }

    fmt.Printf("[%s] Analysis complete. Analyzed %v documents. Elapsed time %v secs.\n", shard.Host, cnt, (time.Now().UnixNano() - start)/1000000000)



}

func removeOrphans(waitGroup *sync.WaitGroup, db,c,u,p string, shard *Shard) {

    fmt.Println("Starting remove routine for shard: [" + shard.Host + "]")
    defer waitGroup.Done()

    hosts := strings.Split(shard.Host[strings.Index(shard.Host, "/") + 1:len(shard.Host)], ",")

    dialInfo := &mgo.DialInfo {
        Addrs: hosts,
        Source: "admin",
        Username: u,
        Password: p,
    }

    session, err := mgo.DialWithInfo(dialInfo)
    if err != nil {
	   panic(err)
    }
    defer session.Close()
    session.SetMode(mgo.Monotonic, true)

    target := session.DB(db).C(c)
    source := session.DB(db).C(c + "_orphans")

    var id Id
    tq := source.Find(bson.M{}).Select(bson.M{"_id":1})
    it := tq.Iter()
    cnt := 0;
    for {
       if !it.Next(&id) {
         break
       } else {
         err = target.RemoveId(id.ID)
         if err != nil {
           fmt.Printf("Fialed to remove orphan document with id; %v from shard [%s]\n", id.ID, shard.Host)
         } else {
           cnt++
         }
       }
    }
    fmt.Printf("Removed %v documents on shard [%s]\n", cnt, shard.Host)
}

func toInt(s string) int {
    i, err := strconv.Atoi(s)
    if err != nil {
      return -1
    } else {
      return i
    }
}

func toFloat(s string) float64 {
    i, err := strconv.ParseFloat(s, 64)
    if err != nil {
        return -1
    } else {
        return i
    }
}
