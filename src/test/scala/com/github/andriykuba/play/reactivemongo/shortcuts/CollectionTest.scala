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

@RunWith(classOf[JUnitRunner])
class CollectionTest 
  extends FlatSpec 
  with Matchers 
  with MockitoSugar 
  with MongoEmbedDatabase 
  with BeforeAndAfterAll
  with OneAppPerSuite{

  val mongoProt = 12345 
  val testDatabaseName = "shortcuts"
  val testCollectionName = "app.users"
  var mongoProps: MongodProps = null
  
  lazy val injector = 
    new GuiceApplicationBuilder()
      .configure(Map(
          "mongodb.uri" -> 
            ("mongodb://localhost:" + mongoProt.toString + "/" + testDatabaseName),
          "mongo-async-driver.akka.loglevel" -> "WARNING"))
      .bindings(new play.modules.reactivemongo.ReactiveMongoModule)
      .injector

  implicit lazy val mongo = injector.instanceOf[ReactiveMongoApi]
  implicit lazy val context = injector.instanceOf[ExecutionContext]
  
  override def beforeAll()  = {
    mongoProps = mongoStart(mongoProt)  
    
    // Prepare test data
    val mongoClient: MongoClient = MongoClient("mongodb://localhost:"+mongoProt.toString)
    val database: MongoDatabase = mongoClient.getDatabase(testDatabaseName)
    val collection: MongoCollection[Document] = database.getCollection(testCollectionName);
    
    val documents = (1 to 100) map { i: Int => Document("name" -> ("name_" + i.toString)) }
    
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
  
  override def afterAll() = { 
    mongoStop(mongoProps) 
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
    object Users extends Collection{
      val collectionName = testCollectionName
    }
    
    val result = for{
      c <- Users.collection()
      count <- c.count()
    } yield {
      c.name should be (testCollectionName)
      count should be (100)
    }   
    
    Await.result(result, 10 seconds)
  }
  
 it should "find all documents"
 it should "find zero or one document"
 it should "find a first document"
 it should "fold the collection"
 it should "fold the collection asynchroniously"
 it should "update the documents"
 it should "remove the doucment"
 it should "get an optional field from a document"
 it should "get a field from a document"
 it should "get a string field from a document"
}