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

`ReactiveMongoApi` and `ExecutionContext` are passed implicitly to all methods but 
`defineSubCollectionName`. ReactiveMongo is asynchronous, 
so all methods but `defineSubCollectionName` return results wrapped in `Future`.

## Methods and properties

### Collection

#### collectionName

The name of the collection in MongoDB

#### defineSubCollectionName

Sugar for creating name of higher detailed collection. For example you need 
to create a collection of user's page visits and you already have the "users" 
collection. Good naming are:

```
users
users.pagevisits
```

So you can do:

```scala
object Users extends Collection{
  val collectionName = "users"
}

object UsersPagevisits extends Collection{
  val collectionName = "users.pagevisits"
}
```

Or:

```scala
object Users extends Collection{
  val collectionName = "users"
}

object UsersPagevisits extends Collection{
  val collectionName = Users.defineSubCollectionName("pagevisits")
}
```

#### collection

Shortcut for getting a collection from a database.

So this string

```scala
mongo.database.map(_.collection[JSONCollection](collectionName))
```

become as short as

```scala
MyCollection.collection()
```
#### fold

Fold the collection. 
A start value and a fold function are set with the `Folder` case class.
Projection and sort criteria can be used as well.

```scala
val folder = Folder(0, (count: Int, doc: JsObject) => {
  count + 1
})

MyCollection.fold(Json.obj("age" -> 20), folder)
```

#### foldM

Fold the collection asynchronously. 
A start value and a fold function are set with the `FolderM` case class.
Projection and sort criteria can be used as well.

```scala
val folderM = FolderM(0, (count: Int, doc: JsObject) => {
  Future(count + 1)
})

MyCollection.foldM(Json.obj("age" -> 20), folderM)
```

### Document

#### all

Find all documents. It returns a list of `JsObject`. 
Projection and sort criteria can be used as well.

```scala
MyCollection.all(Json.obj("age" -> 30))
```

#### one

Find zero or one document. 
It throw an exception if more than one document found. 
Projection can be used as well.

```scala
MyCollection.one(Json.obj("name" -> "Adam Smith"))
```

#### first

Find zero or one document. 
It returns first document if more than one document found. 
Projection and sort criteria can be used as well.

```scala
MyCollection.first(Json.obj("age" -> 40))
```

### Field

#### fieldOpt[T]

#### field[T]

#### fieldStringOrEmpty

### Update/Create

#### update

### Remove

#### remove