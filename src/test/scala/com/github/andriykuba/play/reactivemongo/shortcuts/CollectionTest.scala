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

@RunWith(classOf[JUnitRunner])
class CollectionTest 
  extends FlatSpec 
  with Matchers 
  with MockitoSugar 
  with MongoEmbedDatabase 
  with BeforeAndAfterAll
  with OneAppPerSuite{

  val mongoProt = 12345
  var mongoProps: MongodProps = null
  
  lazy val injector = 
    new GuiceApplicationBuilder()
      .configure(Map(
          "mongodb.uri" -> 
            ("mongodb://localhost:" + mongoProt.toString + "/shortcuts"),
          "mongo-async-driver.akka.loglevel" -> "WARNING"))
      .bindings(new play.modules.reactivemongo.ReactiveMongoModule)
      .injector

  implicit lazy val mongo = injector.instanceOf[ReactiveMongoApi]
  implicit lazy val context = injector.instanceOf[ExecutionContext]
  
  override def beforeAll()  = {
    mongoProps = mongoStart(mongoProt)    
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
    object App extends Collection {
      val collectionName = "app"  
    }
    
    object Users extends Collection{
      val collectionName = App.defineSubCollectionName("users")
    }
    
    Users.collection().map(c => c.name should be ("app.users"))
  }
}