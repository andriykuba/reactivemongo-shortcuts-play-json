# Reactivemongo Shortcuts for Play and JSON

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.andriykuba/reactivemongo-shortcuts-play-json/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.andriykuba/reactivemongo-shortcuts-play-json)

Reduce code for the common cases of ReactiveMongo usage in Play framework, 
Scala language. 

#### Table of Contents
- [Install](#install)
- [Usage](#usage)
- [Methods and properties](#methods-and-properties)
  - [Collection](#collection)
    - [collectionName](#collectionname)
    - [defineSubCollectionName](#definesubcollectionname)
    - [collection](#collection-1)
    - [count](#count)
    - [fold](#fold)
    - [foldM](#foldm)
  - [Document](#document)
    - [all](#all)
    - [one](#one)
    - [first](#first)
    - [first in a set of collections](#first-in-a-set-of-collections)    
  - [Field](#field)
    - [fieldOpt[T]](#fieldoptt)
    - [field[T]](#fieldt)
    - [fieldStringOrEmpty](#fieldstringorempty)
  - [Update/Create](#updatecreate)
    - [update](#update)
  - [Remove](#remove)
    - [remove](#remove-1)
- [Field shortcuts] (#field-shortcuts)    
    
## Install

Add the library in `built.sbt`
```scala
libraryDependencies += "com.github.andriykuba" % "play-handlebars" % "2.5.7" 
```

## Usage

Suppose, we want to get some field from all documents. 
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

To start use shortcuts just extend `com.github.andriykuba.play.reactivemongo.shortcuts.Collection`

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

#### count

Shortcut for getting a number of documents in the collection.

So this string

```scala
mongo.database.map(_.collection[JSONCollection](collectionName)).flatMap(c => c.count())
```

become as short as

```scala
MyCollection.count()
```

#### fold

Fold the collection. 
A start value and a fold function are set with the `Folder` case class.
Projection and sort can also be used.

```scala
val folder = Folder(0, (count: Int, doc: JsObject) => {
  count + 1
})

MyCollection.fold(Json.obj("age" -> 20), folder)
```

#### foldM

Fold the collection asynchronously. 
A start value and a fold function are set with the `FolderM` case class.
Projection and sort can also be used.

```scala
val folderM = FolderM(0, (count: Int, doc: JsObject) => {
  Future(count + 1)
})

MyCollection.foldM(Json.obj("age" -> 20), folderM)
```

### Document

#### all

Find all documents. It returns a list of `JsObject`. 
Projection and sort can also be used.

```scala
MyCollection.all(Json.obj("age" -> 30))
```

#### one

Find zero or one document. 
It throw an exception if more than one document found. 
Projection can also be used.

```scala
MyCollection.one(Json.obj("name" -> "Adam Smith"))
```

#### first

Find zero or one document. 
It returns first document if more than one document found. 
Projection and sort can also be used.

```scala
MyCollection.first(Json.obj("age" -> 40))
```

#### first in a set of collections

Look for the first document in a set of collections with different selects.

Scenario to use - check if there is some document in a set of collection that 
meets the criteria.

```scala
Collection.first(List(
    (TrainsCollection, Json.obj("price" -> Json.obj("$lte" -> 100))),
    (BusesCollection, Json.obj("price" -> Json.obj("$lte" -> 100))),
    (PlanesCollection, Json.obj("price" -> Json.obj("$lte" -> 100)))))
```

### Field

#### fieldOpt[T]

Get an optional field from a single document.
Throw an exception if more than one document is found.
Field type `T` is obligatory.

```scala
MyCollection.fieldOpt[Int](Json.obj("name"->"Adam Smith"), "age")
```

#### field[T]

Get a field from a single document. It throws an exception if no document 
or field are found.
Throw an exception if more than one document is found.
Field type `T` is obligatory.

```scala
MyCollection.field[Int](Json.obj("name"->"Adam Smith"), "age")
```

#### fieldStringOrEmpty

Get a string field from a single document or empty string 
if no document or file was found. 
Throw an exception if more than one document is found.

```scala
MyCollection.fieldStringOrEmpty(Json.obj("name"->"Adam Smith"), "address")
```

### Update/Create

#### update

Update matching document (using `findAndModify`). 
This method is also used for document creation (`upsert = true`).

```scala
MyCollection.update(
  Json.obj("name"->"Adam Smith"), 
  Json.obj("$set" -> Json.obj("age" -> 80)))  
  
  
MyCollection.update(
  mySelector, 
  myDocument,
  upsert = true)   
```

### Remove

#### remove

Remove matching document (using `findAndModify`).
Return `true` if document was removed or `false` if it was not found.

```scala
MyCollection.remove(Json.obj("name"->"Adam Smith"))  
```   
## Field Shortcuts

In beta, look the source code of the `FieldShortcuts` object