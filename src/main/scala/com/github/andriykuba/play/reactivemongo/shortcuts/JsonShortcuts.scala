package com.github.andriykuba.play.reactivemongo.shortcuts

import scala.reflect.ClassTag
import scala.reflect.classTag

import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.JsPath
import play.api.libs.json.JsNull
import play.api.libs.json.JsArray

object JsonShortcuts {
    
  /**
   * Gets the value from the `doc` object by the `path`.
   * 
   * `path` could contains dots, like `root.some.value`.
   * 
   * @param doc  JSON object where the path will be looked  
   * @param path path to look, sub-objects divided by dots like `root.some.value` 
	 *
   * @return  value found or `JsNull`. Just like the trivial play JSON lookup
   */
  def valueByPath(doc: JsObject, path: String): JsValue = {
    val components = path.split("\\.")
    val jsPath = components.tail.foldLeft(JsPath \ components.head) { _ \ _ }
    
    jsPath(doc) match{
      case Nil => JsNull
      case x::xs => x
    }
  }  
  
  def typing[T: ClassTag](value: Option[JsValue]) = value match {
    case Some(v) => classTag[T].runtimeClass match{
      case ClassOf.string  => v.asOpt[String].asInstanceOf[Option[T]]
      case ClassOf.jsObject => v.asOpt[JsObject].asInstanceOf[Option[T]]
      case ClassOf.jsArray => v.asOpt[JsArray].asInstanceOf[Option[T]]
      case ClassOf.int => v.asOpt[Int].asInstanceOf[Option[T]]
      case ClassOf.double => v.asOpt[Double].asInstanceOf[Option[T]]
      case ClassOf.bigDecimal => v.asOpt[BigDecimal].asInstanceOf[Option[T]] 
      case ClassOf.boolean => v.asOpt[Boolean].asInstanceOf[Option[T]] 
    }
    case None => None
  }
  
  /**
   * Cash of class values for the type resolving of JSON fields.
   */
  private[shortcuts] object ClassOf{
    val string = classOf[String]
    val jsObject = classOf[JsObject]
    val jsArray = classOf[JsArray]
    val int = classOf[Int]
    val double = classOf[Double]
    val bigDecimal = classOf[BigDecimal]
    val boolean = classOf[Boolean]    
  }
}