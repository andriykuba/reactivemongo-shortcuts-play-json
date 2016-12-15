package com.github.andriykuba.play.reactivemongo.shortcuts.exceptions

/**
 * More than one document found, where only one must exist.
 */
case class NotUniqueDocumentException(message: String) 
  extends Exception(message)