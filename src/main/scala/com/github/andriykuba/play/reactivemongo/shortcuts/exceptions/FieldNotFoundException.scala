package com.github.andriykuba.play.reactivemongo.shortcuts.exceptions

/**
 * Document field was not found.
 */
case class FieldNotFoundException(message: String) 
  extends Exception(message)