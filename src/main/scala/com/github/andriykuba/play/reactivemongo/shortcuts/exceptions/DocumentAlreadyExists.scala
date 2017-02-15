package com.github.andriykuba.play.reactivemongo.shortcuts.exceptions

import play.api.libs.json.JsObject

/**
 * More than one document found, where only one must exist.
 */
case class DocumentAlreadyExists(val oldDocument: JsObject) 
  extends Exception(oldDocument.toString())