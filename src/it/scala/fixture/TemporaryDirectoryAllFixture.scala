package fixture

import org.scalatest.{ BeforeAndAfterAll, Suite }

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{ FileVisitResult, Files, Path, SimpleFileVisitor }

/** Temporary Directory Fixture which provides a new temporary directory. It starts beforeAll tests and stops afterAll
  * tests.
  */
trait TemporaryDirectoryAllFixture extends BeforeAndAfterAll { this: Suite =>

  private var temporaryDirectory: Option[Path] = None

  override protected def beforeAll(): Unit = {
    this.temporaryDirectory = Some(Directory.createTemporaryDirectory())
    super.beforeAll() // NOTE(AR) To be stackable, we must call super.beforeAll
  }

  override protected def afterAll(): Unit =
    try
      super.afterAll() // NOTE(AR) To be stackable, we must call super.afterAll
    finally {
      this.temporaryDirectory.map(Directory.deleteDirectory(_))
      this.temporaryDirectory = None
    }

  /** Get temporary directory.
    *
    * @return
    *   the API Service
    */
  def getTemporaryDirectory() = this.temporaryDirectory.get
}

object Directory {

  /** NOTE(AR) DeleteDirVisitor has no state so it is safe to have only a singleton of it.
    */
  private val DELETE_DIR_VISITOR = new DeleteDirVisitor()

  /** Create a new temporary directory.
    *
    * @return
    *   the path to the new temporary directory.
    */
  @throws[IOException]
  def createTemporaryDirectory(): Path =
    Files.createTempDirectory("omega.services-it-tmp-dir")

  /** Delete a directory and all of its contents.
    *
    * @param directory
    *   the directory to delete
    */
  @throws[IOException]
  def deleteDirectory(directory: Path): Unit = {
    Files.walkFileTree(directory, DELETE_DIR_VISITOR)
    () // Explicitly return unit
  }

  /** Recursively deletes all files and folders within a directory and the deletes the directory itself.
    */
  private class DeleteDirVisitor extends SimpleFileVisitor[Path] {
    @throws[IOException]
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      Files.deleteIfExists(file)
      FileVisitResult.CONTINUE
    }

    @throws[IOException]
    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      if (exc != null) {
        throw exc
      }
      Files.deleteIfExists(dir)
      FileVisitResult.CONTINUE
    }
  }
}
