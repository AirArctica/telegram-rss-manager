package com.github.telegram_bots.own

import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.stream.Materializer
import akka.stream.scaladsl.Sink

import scala.collection.SortedMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Implicits {
  implicit class ConvertToOption[T](t: T) { def ? = Option(t) }

  implicit class ToSortedMap[A,B](tuples: TraversableOnce[(A, B)])(implicit ordering: Ordering[A]) {
    def toSortedMap = SortedMap(tuples.toSeq: _*)
  }

  implicit class ExtendedString(str: String) {
    def optionIfBlank: Option[String] = {
      val trimmed = str.trim
      if (trimmed.isEmpty) Option.empty
      else Option(trimmed)
    }
  }

  implicit class ExtendedHttpResponse(response: HttpResponse)(implicit materializer: Materializer, executionContext: ExecutionContext) {
    def getBody: Future[String] =
      response.entity.dataBytes.runWith(Sink.head).map(_.utf8String)

    def decode: HttpResponse = {
      val decoder = response.encoding match {
        case HttpEncodings.gzip ⇒ Gzip
        case HttpEncodings.deflate ⇒ Deflate
        case HttpEncodings.identity ⇒ NoCoding
      }

      decoder.decodeMessage(response)
    }
  }

  implicit class ExtendedFuture[T](future: Future[T])(implicit executionContext: ExecutionContext) {
    def doOnNext(callback: T => Unit): Future[T] = future.map(e => { callback(e); e })

    def doOnComplete(callback: T => Unit): Future[T] = {
      future.onComplete {
        case Success(element) => callback(element)
        case _ =>
      }
      future
    }

    def doOnError(callback: Throwable => Unit): Future[T] = {
      future.onComplete {
        case Failure(e) => callback(e)
        case _ =>
      }
      future
    }
  }
}
