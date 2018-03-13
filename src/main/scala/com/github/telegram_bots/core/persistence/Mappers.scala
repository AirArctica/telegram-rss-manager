package com.github.telegram_bots.core.persistence

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId}

import com.github.telegram_bots.core.domain.{Channel, PostType, PresentPost}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, JdbcType}

object Mappers {
  def enumMapper(enum: Enumeration): JdbcType[enum.Value] = MappedColumnType.base[enum.Value, String](
    _.toString,
    enum.withName(_)
  )

  implicit val localDateTimeMapper: JdbcType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    date => Timestamp.from(date.atZone(ZoneId.systemDefault()).toInstant),
    _.toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime
  )

  implicit val postTypeMapper: JdbcType[PostType.Value] = enumMapper(PostType)

  implicit val postMapper: GetResult[PresentPost] = GetResult(r => PresentPost(
    r.<<,
    PostType.withName(r.<<),
    r.<<,
    r.<<?,
    r.<<[java.sql.Timestamp].toLocalDateTime,
    r.<<?,
    r.<<,
    r.<<
  ))

  implicit val channelMapper: GetResult[Channel] = GetResult(r => Channel(r.<<, r.<<, r.<<))
}