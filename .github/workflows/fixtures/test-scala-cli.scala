//> using dep "com.armanbilge::porcupine::0.0.1"
//> using platform scala-native
//> using scala 3.3.1
//> using nativeVersion 0.4.17

import porcupine.*
import cats.effect.IOApp
import cats.effect.IO
import cats.syntax.all.*
import scodec.bits.ByteVector

import Codec.*

object Test extends IOApp.Simple:
  val run =
    Database
      .open[IO](":memory:")
      .use: db =>
        db.execute(sql"create table porcupine (n, i, r, t, b);".command) *>
          db.execute(
            sql"insert into porcupine values(${`null`}, $integer, $real, $text, $blob);".command,
            (None, 42L, 3.14, "quill-pig", ByteVector(0, 1, 2, 3))
          ) *>
          db.unique(
            sql"select b, t, r, i, n from porcupine;"
              .query(blob *: text *: real *: integer *: `null` *: nil)
          ).flatTap(IO.println)
      .void
end Test
