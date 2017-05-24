package com.github.andriykuba.play.reactivemongo.shortcuts

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import reactivemongo.api.CursorProducer
import reactivemongo.api.Cursor
import play.api.libs.json.JsObject

import com.github.andriykuba.play.reactivemongo.shortcuts.exceptions.CursorFails._
import com.github.andriykuba.play.reactivemongo.shortcuts.exceptions.NotUniqueDocumentException

import com.github.andriykuba.play.reactivemongo.shortcuts.Collection.Folder
import com.github.andriykuba.play.reactivemongo.shortcuts.Collection.FolderM

trait CursorProducerEnchanceImplicit{
 /**
   * Shortcuts of the Cursor
   */
  private[shortcuts] implicit class CursorProducerEnchance(
      cursorProducer: Future[Cursor[JsObject]]){
  
    /**
     * Collection of all founded documents.
     */
    def all()(implicit ec: ExecutionContext) = 
      cursorProducer.flatMap(_.collect[List](Int.MaxValue, errorHandlerList()))
        
    /**
     * Exactly zero or one document must be founded. 
     * Throw exception if founded more than one document. 
     * Usable for select by _id or other unique keys. 
     * It detects possible data inconsistency. 
     */
    def one()(implicit ec: ExecutionContext) = 
      cursorProducer.flatMap(_
        .collect[List](-1, errorHandlerList())
        .map(docs => if(docs.isEmpty){
          None 
        } else if (docs.tail.isEmpty){
          Some(docs.head)
        } else{
          // more than one document found
          throw new NotUniqueDocumentException("There are " + docs.size + " documents.")
        }))

    /**
     * The first document from the founded collection of documents.
     */
    def first()(implicit ec: ExecutionContext) = 
      cursorProducer.flatMap(_
        .collect[List](1, errorHandlerList())
        .map(docs => if(docs.isEmpty) None else Some(docs.head)))
    
    /**
     * Fold the collection of documents to one value.  
     */
    def fold[P](f: Folder[P, JsObject])(implicit ec: ExecutionContext) = 
      cursorProducer.flatMap(_
        .foldWhile(f.start, f.max)((p: P, n: JsObject) => Cursor.Cont(f.fold(p, n))))

    /**
     * Fold the collection of documents to one value.  
     */        
    def foldM[P](f: FolderM[P, JsObject])(implicit ec: ExecutionContext) = 
      cursorProducer.flatMap(_
        .foldWhileM(f.start, f.max)((p: P, n: JsObject) => 
          f.fold(p, n).map(r => Cursor.Cont(r))))    
  }
}