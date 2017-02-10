package com.github.andriykuba.play.reactivemongo.shortcuts

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

import com.github.andriykuba.play.reactivemongo.shortcuts.exceptions.FieldNotFoundException

import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import reactivemongo.api.CursorProducer

/**
 * Shortcut of the `JSONCollection` 
 */
trait Collection extends CursorProducerEnchanceImplicit{
  import Collection._
  
  /**
   * Current collection name
   */
  def collectionName: String
  
  /**
   * Define the new collection name that is prefixed by this name.
   * 
   * For example this collection is named "application", then 
   * `defineSubCollectionName("users")` will return  "application.users".
   * 
   * @param name  the name of the sub collection 
   * @return  string that consists from the current collections name and passed name
   */
  def defineSubCollectionName(name: String): String = collectionName + "." + name
    
  /**
   * Get collection in one method call
   * 
   * @return  the collection, wrapped in a `Future`
   */
  def collection()
                (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
                : Future[JSONCollection] = 
    mongo.database.map(_.collection[JSONCollection](collectionName))  
  
    
  /**
   * Shortcut for the `collection.count()` method
   */
  def count()(implicit mongo: ReactiveMongoApi, ec: ExecutionContext): Future[Int] = 
    collection().flatMap(c => c.count())   
    
  /**
   * Find in one method call. 
   * It's half-cooked method, that returns `CursorProducer`. 
   *
	 * For the `JsObject` ('s) result:
	 * 
   * Use the `all(JsObject, JsObject)` to find a set of document 
   * 
   * Use the `one(JsObject, JsObject)` to find one.
   * 
   * @param selector  the document selector
   * @param Option[projection]  the projection document to select only 
   * 														a subset of each matching documents (default: None)
   * @param Option[sort]  the document indicating the sort criteria (default: None)
   *    
   * @return  `CursorProducer`, wrapped in a `Future` 
   */    
  def find(
      selector: JsObject, 
      projection: Option[JsObject] = None, 
      sort: Option[JsObject] = None)
      (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
      : Future[CursorProducer[JsObject]#ProducedCursor] = 
    collection().map{
      sort match{
        case Some(s) => findProjection(_, selector, projection).sort(s).cursor[JsObject]()
        case None => findProjection(_, selector, projection).cursor[JsObject]()
      }
    }
  
  private def findProjection(c: JSONCollection, s: JsObject, p: Option[JsObject]) =
    p match{
        case Some(p) => c.find(s, p)
        case None => c.find(s)
    }
    
  /**
   * Find all documents in one method call. 
   * 
   * @param selector  the document selector
   * @param Option[projection]  the projection document to select only 
   * 														a subset of each matching documents (default: None)
   * @param Option[sort]  the document indicating the sort criteria (default: None)
   * @return  a list of documents, wrapped in a `Future`
   */  
  def all(selector: JsObject, projection: Option[JsObject] = None, sort: Option[JsObject] = None)
         (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
         : Future[List[JsObject]] =
    find(selector, projection, sort).all()

  /**
   * Find zero or one document in one method call. 
   * Exception will be thrown if more than one document found.
   * 
   * @param selector  the document selector
   * @param Option[projection]  the projection document to select only 
   * 														a subset of each matching documents (default: None)
   * @return  document, optional, wrapped in a `Future`
   */    
  def one(selector: JsObject, projection: Option[JsObject] = None)
         (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
         : Future[Option[JsObject]]=
    find(selector, projection).one()

  /**
   * The first document from the founded collection of documents.
   * 
   * @param selector  the document selector
   * @param Option[projection]  the projection document to select only 
   * 														a subset of each matching documents (default: None)
   * @param Option[sort]  the document indicating the sort criteria (default: None)
   * @return  first found document, optional, wrapped in a `Future`
   */    
  def first(selector: JsObject, projection: Option[JsObject] = None, sort: Option[JsObject] = None)
           (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
           :Future[Option[JsObject]]=
    find(selector, projection, sort).first()
    
  /**
   * Do `foldWhile` on the collection in one method call. 
   * 
   * @param selector  the document selector
   * @param folder the `Folder` object with `start` value and `fold` function 
   * @param Option[projection]  the projection document to select only 
   * 														a subset of each matching documents (default: None)
   * @param Option[sort]  the document indicating the sort criteria (default: None)
   * @return  final result of a `fold` function, wrapped in a `Future`
   */ 
  def fold[P](
      selector: JsObject, 
      folder: Folder[P, JsObject],
      projection: Option[JsObject] = None, 
      sort: Option[JsObject] = None)
      (implicit mongo: ReactiveMongoApi, ec: ExecutionContext) 
      : Future[P] =
    find(selector, projection, sort).fold(folder)

  /**
   * Do `foldWhileM` on the collection in one method call. 
   * 
   * @param selector  the document selector
   * @param folder the `FolderM` object with `start` value and `fold` function 
   * @param Option[projection]  the projection document to select only 
   * 														a subset of each matching documents (default: None)
   * @param Option[sort]  the document indicating the sort criteria (default: None)
   * @return  final result of a `fold` function, wrapped in a `Future`
   */     
  def foldM[P](
      selector: JsObject, 
      folder: FolderM[P, JsObject],
      projection: Option[JsObject] = None, 
      sort: Option[JsObject] = None)
      (implicit mongo: ReactiveMongoApi, ec: ExecutionContext) 
      : Future[P] =
    find(selector, projection, sort).foldM(folder) 
    
  /**
   * Finds some matching document, and updates it (using `findAndModify`).
   * 
   * @see <a href="https://docs.mongodb.com/manual/reference/command/findAndModify">
   * findAndModify in the MongoDB documentation</a>
   * 
   * @param selector  the document selector
   * @param update  the update to be applied
   * @param fetchNewObject  the command result must be the new object instead of the old one.
   * @param upsert  if true, creates a new document if no document is matching, 
   * 								otherwise if at least one document matches, an update is applied
   * @return  a result document as `JsObject`, wrapped in `Future`
   */
  def update(
      selector: JsObject, 
      update: JsObject, 
      fetchNewObject: Boolean = false,
      upsert: Boolean = false)
      (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
      : Future[Option[JsObject]] = 
    collection().flatMap(_
      .findAndUpdate(
          selector = selector, 
          update = update, 
          fetchNewObject, 
          upsert).map(_.result[JsObject]))

  /**
   * Inserts a document into the collection and wait for the results.
   * 
   * The command is success if it was not fail. 
   * 
   * @see <a href="http://reactivemongo.org/releases/0.12/api/index.html#reactivemongo.api.commands.WriteResult">
   * WriteResult in the Reactivemongo documentation</a>
   * 
   * @param document  the document to insert
   * @return  a `WriteResult` object, wrapped in `Future` 
   * 								
   */          
  def insert(
      document: JsObject)
      (implicit mongo: ReactiveMongoApi, ec: ExecutionContext) =
    collection().flatMap(_.insert(document)) 
          
  /**
   * Finds some matching document, and removes it (using `findAndModify`).
   * 
   * @param selector  the document selector
   * @return  true if document was removed or false if it was not found, wrapped in `Future`
   */
  def remove(selector: JsObject)
            (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
            : Future[Boolean] = 
    collection().flatMap(_.findAndRemove(selector).map(_.value match{
      case Some(doc) => true
      case None => false
    }))
      
  /**
   * Get a field from a single document. 
   * 
   * The single document is taken by `one` method. 
   * 
   * @param selector  the document selector
   * @param name  the name of the field
   * @return optional field value in a `JsValue` form, wrapped in a `Future`
   */
  def fieldJsValueOpt(selector: JsObject, name: String) 
           (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
           : Future[Option[JsValue]]=
    one(selector, Some(Json.obj(name  -> 1)))
      .map{
        case Some(doc) => Some(JsonShortcuts.valueByPath(doc, name))
        case None => None 
      }

  /**
   * Get a field from a single document.
   * 
   * Field type `T` is obligatory 
   * 
   * @param [T]  type of the field
   * @param selector  the document selector
   * @param name  the name of the field
   * @return optional field value as `T`, wrapped in a `Future`
   * 
   */
  def fieldOpt[T: ClassTag](selector: JsObject, name: String) 
                           (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
                           : Future[Option[T]] =
  fieldJsValueOpt(selector, name).map(JsonShortcuts.typing[T])

  /**
   * Get a field from a single document.
   * 
   * Field type `T` is obligatory 
   * 
   * @param [T]  type of the field
   * @param selector  the document selector
   * @param name  the name of the field
   * @return field value as `T`, wrapped in a `Future`
   * 
   */  
  def field[T: ClassTag](selector: JsObject, name: String) 
               (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
               : Future[T] =
    fieldOpt[T](selector, name).map{
      case Some(v) => v
      case None => throw FieldNotFoundException("No such field: " + name)
    }
  
  /**
   * Get a string field from a single document or empty string 
   * if no document or file was found.
   * 
   * Empty string returned if no document was found 
   * or no field was found in the document.
   * 
   * @param selector  the document selector
   * @param name  the name of the field
   * @return field value or empty string, wrapped in a `Future`
   */
  def fieldStringOrEmpty(selector: JsObject, name: String)
                        (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
                        : Future[String] =
    fieldOpt[String](selector, name).map(_.getOrElse(""))
}

object Collection{
  
  /**
   * Describe the `foldWhile` function. 
   * 
   * @param start  the initial value of `foldWhile`
   * @param fold  folding function of `foldWhile`
   * @param max  the maximum number of documents to be retrieved (-1 for unlimited)
   */
  case class Folder[P, JsObject](start: P, fold: (P, JsObject) => P, max: Int = -1)

  /**
   * Describe the `foldWhileM` function. 
   * 
   * @param start  the initial value of `foldWhileM`
   * @param fold  folding function of `foldWhileM`
   * @param max  the maximum number of documents to be retrieved (-1 for unlimited)
   */
  case class FolderM[P, JsObject](start: P, fold: (P, JsObject) => Future[P], max: Int = -1)
   
  /**
   * Look for the first document in a set of collections with different selects
   * 
   * @param collections  list of the collection objects and correspondent selects
   * @return  option JSON document wrapped in a `Future`
   */
  def first(collections: List[(Collection, JsObject)])
           (implicit mongo: ReactiveMongoApi, ec: ExecutionContext)
           :Future[Option[JsObject]] = {
    
    def dig(collections: List[(Collection, JsObject)]):Future[Option[JsObject]] = 
      collections match {
        // No documents was found
        case Nil => Future.successful(None)
        case h :: t => h._1.first(h._2).flatMap{
          // Some document is found in the collection
          case Some(d) => Future.successful(Option(d))
          // No document found in the collection, look further
          case None => dig(t)
        }
      }
    dig(collections)
  }
             
    
}