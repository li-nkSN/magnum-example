package org.li_nk.magnum.example

import com.augustnagro.magnum.magzio.Transactor
import com.augustnagro.magnum.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import munit.{AnyFixture, FunSuite}
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.utility.DockerImageName
import zio.{Scope, Unsafe, ZLayer}

import java.nio.file.{Files, Path}
import scala.util.Using
import scala.util.Using.Manager

class PgZioTests extends FunSuite, TestContainersFixtures:

  val pgContainer = ForAllContainerFixture(
    PostgreSQLContainer
      .Def(dockerImageName = DockerImageName.parse("postgres:17.0"))
      .createContainer()
  )

  override def munitFixtures: Seq[AnyFixture[_]] =
    super.munitFixtures :+ pgContainer

  def xa(): Transactor =
    val ds = PGSimpleDataSource()
    val pg = pgContainer()
    ds.setUrl(pg.jdbcUrl)
    ds.setUser(pg.username)
    ds.setPassword(pg.password)
    val tableDDLs = Vector(
      "/pg/car.sql",
      "/pg/person.sql",
      "/pg/my-user.sql",
      "/pg/no-id.sql",
      "/pg/big-dec.sql"
    ).map(p => Files.readString(Path.of(getClass.getResource(p).toURI)))

    Manager(use =>
      val con = use(ds.getConnection)
      val stmt = use(con.createStatement)
      for ddl <- tableDDLs do stmt.execute(ddl)
    ).get
    // todo unsafe
    Unsafe.unsafe { implicit unsafe =>
      zio.Runtime.default.unsafe
        .run(
          Transactor.layer(ds)
            .build(Scope.global)
            .map(_.get)
        )
        .getOrThrow()
    }
  end xa
end PgZioTests
