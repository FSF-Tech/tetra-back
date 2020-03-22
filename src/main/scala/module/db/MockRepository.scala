package org.fsf.tetra.module.db

import zio.{ ZIO, ZLayer }
import org.fsf.tetra.model.database.User
import org.fsf.tetra.model.{ ExpectedFailure }
import org.fsf.tetra.module.db.userRepository._

object mockRepository {

  val live: ZLayer[MockR, Nothing, UserRepository] = ZLayer.fromFunction { ref: MockR =>
    new UserRepository.Service {
      def get(id: Long): ZIO[Any, ExpectedFailure, Option[User]] =
        for {
          user <- ref.get.map(_.get(id))
          out <- user match {
                  case Some(s) => ZIO.some(s)
                  case None    => ZIO.none
                }
        } yield out

      def create(user: User): ZIO[Any, ExpectedFailure, Unit] = ref.update(map => map.+(user.id -> user)).unit

      def delete(id: Long): ZIO[Any, ExpectedFailure, Unit] = ref.update(map => map.-(id)).unit
    }
  }
}