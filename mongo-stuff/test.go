package main

import (
   "fmt"
   "time"
   "strconv"
   "os"
//   "strings"
   "reflect"
   "os/exec"
   "labix.org/v2/mgo"
   "labix.org/v2/mgo/bson"
)

type Id struct {
  ID string `bson:"_id"`
}

func main() {
  path, err := exec.LookPath("mongod")
  if err != nil {
     fmt.Printl("Mongo is not found in PATH. Make sure mongod is in PATH.
     Exiting now.")
     os.Exit(1)
  }



  err := exec.Run("")
  a:= "abc"
  b:="abc"
  c := a == b
  fmt.Printf("%v\n",c)
  se, _ := mgo.Dial("localhost:27017")
  db := se.DB("test")
  r := bson.M{}
  _ = db.Run(bson.M{"_hashBSONElement":"asdf"}, &r)
  fmt.Println(r["out"])

  fmt.Println(reflect.TypeOf(r["out"]))
/*
  str := "rep1A/greendb301p.stress.ch3.s.com:29001,greendb310p.stress.ch3.s.com:29002"
  s := str[strings.Index(str,"/") + 1:len(str)]
  hosts := strings.Split(s, ",")

  dialInfo := &mgo.DialInfo {
     Addrs: hosts,
     Source: "admin",
     Username: "gborphan",
     Password: "gb4test",
  }

  session, err := mgo.DialWithInfo(dialInfo)
  if err != nil {
    panic(err)
  }

  c := session.DB("offer").C("offer")
  var id Id
  q := c.Find(bson.M{}).Select(bson.M{"_id":1})
  it := q.Iter()
  cnt := 0
  for {
     if !it.Next(&id) || cnt == 100 {
         break
     } else {
         cnt++;
         fmt.Println("{_id:" + id.ID + "}")
     }
  }

*/

  ii := 92834723487
  start := time.Now().UnixNano()
  for i:= 0; i<15000000; i++ {
     if ii <= 1 && ii > 9887243487987 {
         print(ii)
    }
  }
  end := time.Now().UnixNano()
  t := end - start
  fmt.Println("Elapsed time: " + strconv.FormatInt(t, 10))}


