package com.github.andriykuba.play.reactivemongo.shortcuts

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest._
import com.github.simplyscala.MongoEmbedDatabase
import com.github.simplyscala.MongodProps
import org.scalatestplus.play.OneAppPerSuite
import play.modules.reactivemongo.ReactiveMongoApi
import scala.concurrent.ExecutionContext
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Configuration
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.Document
import reactivemongo.bson.BSONDocument
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json.Json
import org.mongodb.scala.Completed
import com.github.andriykuba.play.reactivemongo.shortcuts.exceptions.NotUniqueDocumentException

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
  val testTimeout = 10 seconds
  
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
      (res: Completed) => println("Documents was inserted"),
      (e: Throwable) => {
        println("Error")
        e.printStackTrace()
      },
      () => {
        println("Completed")
        mongoClient.close()
    })
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
      
  it should "find a first document"
  it should "fold the collection"
  it should "fold the collection asynchroniously"
  it should "update the documents"
  it should "remove the doucment"
  it should "get an optional field from a document"
  it should "get a field from a document"
  it should "get a string field from a document"
  it should "throw error"
}