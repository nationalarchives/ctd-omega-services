package uk.gov.nationalarchives.omega.api

import cats.effect.std.Supervisor
import cats.effect.{ExitCode, IO, IOApp}

class MockApp2 extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val service = new MockService(failStart = false, failStop = false)

    Supervisor[IO](await = true).use { supervisor =>
      val oc = for {
        fiber <- supervisor.supervise(service.start *> IO.pure(ExitCode.Success))
        outcome <- fiber.joinWith(IO.pure(ExitCode.Success))
      } yield outcome
      oc.handleErrorWith(_ => IO.pure(ExitCode.Error))
    }

  }
}
