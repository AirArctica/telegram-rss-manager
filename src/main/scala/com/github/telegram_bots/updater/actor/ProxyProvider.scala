package com.github.telegram_bots.updater.actor

import java.net.InetSocketAddress
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import akka.NotUsed
import akka.actor.Actor
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.scaladsl.{Sink, Source}
import com.github.telegram_bots.core.Implicits._
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.config.ConfigProperties
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.updater.actor.ProxyProvider._
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}

class ProxyProvider(config: Config) extends Actor with ReactiveActor {
  val props = new Properties(config)
  val proxies: BlockingQueue[Proxy] = new LinkedBlockingQueue()
  var running: Boolean = false

  override def receive: Receive = {
    case Get =>
      if (!running && proxies.size() <= props.minSize) {
        running = true
        downloadProxies
          .runWith(Sink.foreach(proxies.offer(_)))
          .doOnComplete { _ => running = false }
      }

      sender ! proxies.poll(timeout.duration._1, timeout.duration._2)
  }

  private def downloadProxies: Source[Proxy, Future[NotUsed]] = {
    val uri = s"http://pubproxy.com/api/proxy?format=txt&type=http&limit=${props.downloadSize}&level=anonymous&https=true&user_agent=true"

    Source.lazilyAsync { () => Http().singleRequest(HttpRequest(uri = uri)) }
      .flatMapConcat(parseResponse)
      .log("downloaded")
      .mapAsyncUnordered(props.downloadSize)(proxy => Future.successful(proxy).zip(checkWorking(proxy)))
      .recover { case _: TimeoutException => (Proxy.EMPTY, false) }
      .filter(_._2)
      .map(_._1)
      .log("checked")
  }

  private def parseResponse(response: HttpResponse): Source[Proxy, Any] = {
    response.getBody
      .mapConcat(_.split(System.lineSeparator()).toStream)
      .map(line => {
        val Array(host, port) = line.split(":")
        Proxy(host, port.toInt)
      })
  }

  private def checkWorking(proxy: Proxy): Future[Boolean] = {
    val request = HttpRequest(uri = "https://t.me/by_cotique/6")

    Http().singleRequest(request, settings = createConnection(proxy))
      .recover { case _ => HttpResponse(status = StatusCodes.Forbidden) }
      .flatMap(_.getBody.runWith(Sink.head))
      .map(_.contains("https://twitter.com/Hagnir/status/771707002632429569"))
  }

  private def createConnection(proxy: Proxy): ConnectionPoolSettings = {
    ConnectionPoolSettings(system)
      .withTransport(ClientTransport.httpsProxy(
        InetSocketAddress.createUnresolved(proxy.host, proxy.port)
      ))
      .withConnectionSettings(ClientConnectionSettings(system)
        .withIdleTimeout(timeout.duration)
      )
  }
}

object ProxyProvider {
  case object Get

  class Properties(root: Config) extends ConfigProperties(root, "akka.actor.self.proxy-provider") {
    val downloadSize: Int = self.getInt("download-size")
    val minSize: Int = self.getInt("min-size")
  }
}
