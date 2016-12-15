package com.github.andriykuba.play.reactivemongo.shortcuts.exceptions

import reactivemongo.api.Cursor
import play.api.libs.json.JsObject

private [shortcuts] object CursorFails {
    /**
   * Error handler of the collection processing, generic.
   */
  def errorHandler[A]() = 
    Cursor.FailOnError((last: A, e: Throwable) => Cursor.Fail(e))
    
  /**
   * Error handler of the collection processing, list of JSON objects.
   */  
  def errorHandlerList() = 
    errorHandler[List[JsObject]]()
}