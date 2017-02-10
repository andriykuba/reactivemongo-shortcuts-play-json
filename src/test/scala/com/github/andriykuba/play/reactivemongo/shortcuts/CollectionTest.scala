package com.github.andriykuba.play.reactivemongo.shortcuts

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.json.JsObject

import play.modules.reactivemongo.ReactiveMongoApi

import org.junit.runner.RunWith

import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar

import org.scalatestplus.play.OneAppPerSuite

import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.Document
import org.mongodb.scala.Completed

import reactivemongo.bson.BSONDocument

import com.github.simplyscala.MongoEmbedDatabase
import com.github.simplyscala.MongodProps

import com.github.andriykuba.play.reactivemongo.shortcuts.exceptions.NotUniqueDocumentException
import com.github.andriykuba.play.reactivemongo.shortcuts.Collection.Folder
import com.github.andriykuba.play.reactivemongo.shortcuts.Collection.FolderM
import com.github.andriykuba.play.reactivemongo.shortcuts.exceptions.FieldNotFoundException
import reactivemongo.core.actors.Exceptions.ClosedException
import com.github.andriykuba.play.reactivemongo.shortcuts.exceptions.DocumentAlreadyExists

@RunWith(classOf[JUnitRunner])
class CollectionTest 
  extends FlatSpec 
  with Matchers 
  with MockitoSugar 
  with MongoEmbedDatabase 
  with BeforeAndAfterAll
  with OneAppPerSuite{

  val testMongoProt = 12345 
  val testDatabaseName = "shortcuts"
  val testCollectionName = "app.users"
  val testTimeout = 15 seconds
  
  lazy val injector = 
    new GuiceApplicationBuilder()
      .configure(Map(
          "mongodb.uri" -> 
            ("mongodb://localhost:" + testMongoProt.toString + "/" + testDatabaseName),
          "mongo-async-driver.akka.loglevel" -> "WARNING"))
      .bindings(new play.modules.reactivemongo.ReactiveMongoModule)
      .injector

  implicit lazy val mongo = injector.instanceOf[ReactiveMongoApi]
  implicit lazy val context = injector.instanceOf[ExecutionContext]
  
  object TestCollection extends Collection{
      val collectionName = testCollectionName
  }
  
  var mongoProps: MongodProps = null

  override def beforeAll()  = {
    mongoProps = mongoStart(testMongoProt)  
    prepareTestData() 
  }
  
  override def afterAll() = { 
    mongoStop(mongoProps) 
  }
  
  def prepareTestData() = {
    val mongoClient: MongoClient = 
    MongoClient("mongodb://localhost:" + testMongoProt.toString)
    
    val database: MongoDatabase = 
      mongoClient.getDatabase(testDatabaseName)
      
    val collection: MongoCollection[Document] = 
      database.getCollection(testCollectionName);
    
    val documents = (1 to 100) map { i: Int => {
      val even = i % 2 == 0
      Document(
          "name" -> ("name_" + i.toString),
          "even" -> even) 
    }}
    
    collection.insertMany(documents).subscribe(
      (res: Completed) => Unit,
      (e: Throwable) => e.printStackTrace(),
      () => mongoClient.close()
    )
  }
  
  "Shorcuts" should "extend collection" in {
    object App extends Collection {
      val collectionName = "app"  
    }
      
    App.collectionName should be ("app")
  }
  
  it should "define sub collection name" in {
    object App extends Collection {
      val collectionName = "app"  
    }
      
    object Users extends Collection{
      val collectionName = App.defineSubCollectionName("users")
    }
    
    Users.collectionName should be ("app.users")
  }
  
  it should "get collection" in {
    val result = for{
      c <- TestCollection.collection()
      count <- c.count()
    } yield {
      c.name should be (testCollectionName)
      count should be (100)
    }   
    
    Await.result(result, testTimeout)
  }
  
  it should "find all documents" in 
    Await.result(
      TestCollection
        .all(Json.obj("even" -> true))
        .map(r => r.size should be (50)), 
      testTimeout)
 
  it should "find a document" in 
    Await.result(
      TestCollection
        .one(Json.obj("name" -> "name_15"))
        .map(r => r.isDefined should be (true)), 
      testTimeout)
  
  it should "not find a document" in 
    Await.result(
      TestCollection
        .one(Json.obj("name" -> "name_150"))
        .map(r => r.isDefined should be (false)), 
      testTimeout)    
  
  it should "throw NotUniqueDocumentException exception" in {
    val exception = intercept[NotUniqueDocumentException]{
      Await.result(
      TestCollection
        .one(Json.obj("even" -> true))
        .map(r => r),
      testTimeout)
    }
    
    exception.getMessage should equal ("There are 50 documents.")
  }
      
  it should "find a first document" in
    Await.result(
    TestCollection
      .first(Json.obj("name" -> "name_15"))
      .map(r => r.isDefined should be (true)), 
    testTimeout)
  
  it should "not find a first document" in 
    Await.result(
      TestCollection
        .first(Json.obj("name" -> "name_150"))
        .map(r => r.isDefined should be (false)), 
      testTimeout) 

  it should "find a first document and not thrown an exceptin" in 
    Await.result(
      TestCollection
        .first(Json.obj("even" -> true))
        .map(r => r.isDefined should be (true)), 
      testTimeout) 
      
  it should "fold the collection" in {
    val folder = Folder(0, (count: Int, doc: JsObject) => {
      count + 1
    })
    
    Await.result(
      TestCollection
        .fold(Json.obj(), folder)
        .map(r => r should be (100)), 
      testTimeout) 
  }
      
  it should "fold the collection asynchroniously" in {
    val folderM = FolderM(0, (count: Int, doc: JsObject) => {
      Future(count + 1)
    })
    
    Await.result(
      TestCollection
        .foldM(Json.obj(), folderM)
        .map(r => r should be (100)), 
      testTimeout) 
  }
  
  it should "get an optional field from a document" in 
    Await.result(
      TestCollection
        .fieldOpt[Boolean](Json.obj("name"->"name_2"), "even")
        .map(r => r.get should be (true)), 
      testTimeout) 
  
  it should "not get an field value if filed is not exist" in 
    Await.result(
      TestCollection
        .fieldOpt[Boolean](Json.obj("name"->"name_2"), "fake")
        .map(r => r.isDefined should be (false)), 
      testTimeout)     
  
  it should "thrown an error if more than one document exist" in {
    val exception = intercept[NotUniqueDocumentException]{
      Await.result(
        TestCollection
          .fieldOpt[Boolean](Json.obj("even"->true), "name")
          .map(r => r), 
        testTimeout)    
    }
    
    exception.getMessage should equal ("There are 50 documents.")
  }   
      
  it should "get a field from a document" in 
    Await.result(
      TestCollection
        .field[Boolean](Json.obj("name"->"name_2"), "even")
        .map(r => r should be (true)), 
      testTimeout) 

  it should "thrown an error if field is not present" in {
    val exception = intercept[FieldNotFoundException]{
      Await.result(
        TestCollection
          .field[Boolean](Json.obj("name"->"name_2"), "fake")
          .map(r => r), 
        testTimeout)    
    }
    
    exception.getMessage should equal ("No such field: fake")
  }  
  
  it should "get a string field from a document" in 
    Await.result(
      TestCollection
        .fieldStringOrEmpty(Json.obj("name"->"name_2"), "name")
        .map(r => r should be ("name_2")), 
      testTimeout) 
  
  it should "get an empty string if field is not present" in 
    Await.result(
      TestCollection
        .fieldStringOrEmpty(Json.obj("name"->"name_2"), "fake")
        .map(r => r should be ("")), 
      testTimeout) 
      
  it should "update only one document" in 
    Await.result(
      TestCollection
        .update(Json.obj("even"->true), Json.obj(
          "$set" -> Json.obj("odd" -> false)))
        .flatMap(r => {
           TestCollection
             .all(Json.obj("odd" -> false))
             .map(r => r.size should be (1))
        }),
      testTimeout) 

  it should "remove only one doucment" in 
    Await.result(
      TestCollection
        .remove(Json.obj("even"->true))
        .flatMap(r => {
           TestCollection
             .all(Json.obj())
             .map(r => r.size should be (99))
        }),
      testTimeout) 
      
  it should "insert a document" in 
    Await.result(
      TestCollection
        .insert(Json.obj("name" -> "unlisted_1"))
        .flatMap(r => {
           TestCollection
             .all(Json.obj("name" -> "unlisted_1"))
             .map(r => r.size should be (1))
        }),
      testTimeout) 
      
  it should "create a document" in 
    Await.result(
      TestCollection
        .createUnique(Json.obj("name" -> "unlisted_2"), Json.obj("name" -> "unlisted_2"))
        .flatMap(r => {
           TestCollection
             .all(Json.obj("name" -> "unlisted_2"))
             .map(r => r.size should be (1))
        }),
      testTimeout)   
      
  it should "throw DocumentAlreadyExists" in {
    val exception = intercept[DocumentAlreadyExists]{
      Await.result(
        TestCollection
          .createUnique(Json.obj("name" -> "unlisted_2"), Json.obj("name" -> "unlisted_2"))
          .flatMap(r => {
             TestCollection
               .all(Json.obj("name" -> "unlisted_2"))
               .map(r => r.size should be (1))
          }),
        testTimeout)   
     }
    
    (exception.oldDocument \ "name").as[String] should be ("unlisted_2")
  }
  
  it should "throw error" in {
    mongo.connection.close()
    
    val exception = intercept[ClosedException]{
      Await.result(
        TestCollection
          .fieldStringOrEmpty(Json.obj("name"->"name_2"), "name")
          .map(r => r should be ("name_2")), 
        testTimeout) 
    }
    
    exception.getMessage should include ("This MongoConnection is closed")
  }
}