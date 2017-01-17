package com.github.andriykuba.play.reactivemongo.shortcuts

import play.api.libs.json.Json
import java.time.Instant
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import scala.reflect.ClassTag

/**
 * Shortcuts for the different operations with fields
 */
trait FieldShortcuts {
  
  object Query{
    val all = Json.obj()
    
    def field(name: String, value: String) = 
      Json.obj(name -> value)
    
    def field(name: String, value: Int) = 
      Json.obj(name -> value)
    
    def field(name: String, value: Boolean) = 
      Json.obj(name -> value)
    
    def field(name: String, value: Instant) = 
      Json.obj(name -> value)
      
    def exist(name: String) = 
      Json.obj(name -> Json.obj("$exists" -> true))
    
    def absent(name: String) = 
      Json.obj(name -> Json.obj("$exists" -> false)) 
    
    def <=(name: String, value: Instant) = 
      Json.obj(name -> Json.obj("$lte" -> value))
    
    def <=(name: String, value: Int) = 
      Json.obj(name -> Json.obj("$lte" -> value))
    
    def >=(name: String, value: Instant) = 
      Json.obj(name -> Json.obj("$gte" -> value))
    
    def >=(name: String, value: Int) = 
      Json.obj(name -> Json.obj("$gte" -> value))
  }
  
  object Command{
    def setNow(name: String) = 
      Json.obj("$set" -> Json.obj(name -> Instant.now()))  
    
    def set(name: String, value: String) = 
      Json.obj("$set" -> Json.obj(name -> value))
    
    def set(name: String, value: Int) = 
      Json.obj("$set" -> Json.obj(name -> value))
    
    def set(name: String, value: Instant) = 
      Json.obj("$set" -> Json.obj(name -> value))  
    
    def set(name: String, value: Boolean) = 
      Json.obj("$set" -> Json.obj(name -> value))    

    def set(name: String, value: JsObject) = 
      Json.obj("$set" -> Json.obj(name -> value))

    def set(value: JsObject) = 
      Json.obj("$set" -> value)
      
    def setOnInsert(value: JsObject) = 
      Json.obj("$setOnInsert" -> value)
        
    def unset(name: String) = 
      Json.obj("$unset" -> Json.obj(name -> ""))
  }
  
  object Sort{
    def descendant(name: String) = Json.obj(name -> -1)
    
    def ascendant(name: String) = Json.obj(name -> 1) 
  }
  
  object Projection{
    def include(name: String*) = Json.toJson(name.map(n => (n -> 1)).toMap).as[JsObject]
  }
  
  /**
   * Allow dot notation
   * 
   * Values.string(jsObject, "some.data.inside")
   * 
   */
  object Value{
    def get[T: ClassTag](j: JsObject, name: String) = 
      JsonShortcuts.typing[T](Some(JsonShortcuts.valueByPath(j, name))).get
      
    def getOpt[T: ClassTag](j: JsObject, name: String) = 
      JsonShortcuts.typing[T](Some(JsonShortcuts.valueByPath(j, name)))  
    
    def string(j: JsObject, name: String) = get[String](j, name)
    
    def stringOrEmpty(j: JsObject, name: String) = getOpt[String](j, name).getOrElse("")
    
    def int(j: JsObject, name: String) = get[Int](j, name)
    
    def boolean(j: JsObject, name: String) = get[Boolean](j, name)
    
    def jsValue(j: JsObject, name: String) = get[JsValue](j, name)
    
    def listString(j: JsObject, name: String) = get[List[String]](j, name)
  }

}