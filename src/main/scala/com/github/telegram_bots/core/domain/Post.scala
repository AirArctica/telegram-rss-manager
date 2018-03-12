package com.github.telegram_bots.core.domain

import com.github.telegram_bots.core.domain.PostType.PostType
import com.github.telegram_bots.core.domain.Types.{ChannelURL, PostID}

abstract sealed class Post(
  val id: PostID,
  val channelLink: ChannelURL
)

case class PresentPost(
  override val id: PostID,
  `type`: PostType,
  content: String,
  fileURL: Option[String] = Option.empty,
  date: Long,
  author: Option[String] = Option.empty,
  override val channelLink: ChannelURL,
  channelName: String
) extends Post(id, channelLink)

case class EmptyPost(
  override val id: PostID,
  override val channelLink: ChannelURL
) extends Post(id, channelLink)

object PostType extends Enumeration {
  type PostType = Value

  val TEXT = Value
  val IMAGE = Value
  val STICKER = Value
  val AUDIO = Value
  val VIDEO = Value
  val FILE = Value
  val GEO = Value
  val CONTACT = Value
}
