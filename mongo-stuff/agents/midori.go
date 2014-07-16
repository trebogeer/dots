package main

import (
    "fmt"
    "labix.org/v2/mgo"
    "labix.org/v2/mgo/bson"
)


func main() {
    session, err := mgo.Dial("mongo1")
    if err != nil {
        panic(err)
    }
    defer session.Close()

    session.SetMode(mgo.Monotonic, true)

    c := session.DB("monitoring").C("test")
    err = c.Insert(bson.M{"hey":"you"})
    if err != nil {
        panic(err)
    }
    fmt.Println("Bye!")
}
