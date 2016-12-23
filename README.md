# Reactivemongo Shortcuts for Play and JSON

Reduce code for the common cases of ReactiveMongo usage in Play framework, 
Scala language. 

#### Table of Contents

## Install

## Usage

Suppose, we want to get some field from all document. 
The "native" ReactiveMongo code will look like:

```scala
def native()(implicit mongo: ReactiveMongoApi):Future[List[JsObject]] = 
  mongo.database
    .map(_.collection[JSONCollection](collectionName)).flatMap(_
      .find(Json.obj(), Json.obj("field" -> 1))
        .cursor[JsObject]()
          .collect[List](-1, 
            Cursor.FailOnError(
              (a: List[JsObject], e: Throwable) => Cursor.Fail(e))))
```

With the Shortcuts it will:

```scala
def shortcuts()(implicit mongo: ReactiveMongoApi): Future[List[JsObject]] = 
  all(Json.obj(), Json.obj("name" -> 1))
```

The Shortcuts reduce method "chaining" to one call 
and use [Play Framework JSON](https://www.playframework.com/documentation/2.5.x/ScalaJson) 
to work with data.

To start use the Shortcuts just extend `com.github.andriykuba.play.reactivemongo.shortcuts.Collection`

```scala
object Users extends Collection{
  val collectionName = "users"
}

...

val allUsersInLondon = Users.all(Json.obj("city" -> "London"))
```

##### collectionName

The name of the collection in MongoDB

##### defineSubCollectionName

##### collection

### Document

##### all

##### one

##### first

##### fold

##### foldM

### Field

##### fieldOpt[T]

##### field[T]

##### fieldStringOrEmpty

### Update/Create

##### update

### Remove

##### remove