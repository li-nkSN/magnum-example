package org.li_nk.magnum.example

import com.augustnagro.magnum.magzio.Transactor
import com.augustnagro.magnum.pg.PgCodec.given
//import com.augustnagro.magnum.pg.enums.PgStringToScalaEnumSqlArrayCodec
import com.augustnagro.magnum._
//import munit.{FunSuite, Location}
import zio.*

import java.sql.Connection
import java.time.OffsetDateTime
import scala.util.{Success, Using}

class ImmutableRepoZioTests extends PgZioTests:
  /*
  def immutableRepoZioTests(
      suite: FunSuite,
      dbType: DbType,
      xa: () => Transactor
  )(using
      Location,
      DbCodec[OffsetDateTime],
      DbCodec[Option[Vector[Long]]]
  ): Unit =


    import suite.*
  */
    val runtime: Runtime[Any] = zio.Runtime.default

    def runIO[A](io: ZIO[Any, Throwable, A]): A =
      Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run(io).getOrThrow()
      }


    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class Car(
        model: String,
        @Id id: Long,
        topSpeed: Int,
        @SqlName("vin") vinNumber: Option[Int],
        created: OffsetDateTime,
        relatedCarIds: Option[Vector[Long]]
    ) derives DbCodec

    val carRepo = ImmutableRepo[Car, Long]
    val car = TableInfo[Car, Car, Long]

    val allCars = Vector(
      Car(
        model = "McLaren Senna",
        id = 1L,
        topSpeed = 208,
        vinNumber = Some(123),
        created = OffsetDateTime.parse("2024-11-24T22:17:30.000000000Z"),
        relatedCarIds = None
      ),
      Car(
        model = "Ferrari F8 Tributo",
        id = 2L,
        topSpeed = 212,
        vinNumber = Some(124),
        created = OffsetDateTime.parse("2024-11-24T22:17:31.000000000Z"),
        relatedCarIds = None
      ),
      Car(
        model = "Aston Martin Superleggera",
        id = 3L,
        topSpeed = 211,
        vinNumber = None,
        created = OffsetDateTime.parse("2024-11-24T22:17:32.000000000Z"),
        relatedCarIds = Some(Vector(101))
      )
    )

    test("count"):
      val count =
        runIO:
          xa().connect:
            carRepo.count
      assert(count == 3L)

    test("existsById"):
      val (exists3, exists4) =
        runIO:
          xa().connect:
            carRepo.existsById(3L) -> carRepo.existsById(4L)
      assert(exists3)
      assert(!exists4)

    test("findAll"):
      val cars =
        runIO:
          xa().connect:
            carRepo.findAll
      assert(cars == allCars)

    test("findById"):
      val (exists3, exists4) =
        runIO:
          xa().connect:
            carRepo.findById(3L) -> carRepo.findById(4L)
      assert(exists3.get == allCars.last)
      assert(exists4 == None)

    test("serializable transaction"):
      val count =
        runIO:
          xa()
            .withConnectionConfig(withSerializable)
            .transact:
              carRepo.count
      assert(count == 3L)

    def withSerializable(con: Connection): Unit =
      con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)

    test("select query"):
      val minSpeed: Int = 210
      val query =
        sql"select ${car.all} from $car where ${car.topSpeed} > $minSpeed"
          .query[Car]
      val result =
        runIO:
          xa().connect:
            query.run()
      assertNoDiff(
        query.frag.sqlString,
        "select model, id, top_speed, vin, created, related_car_ids from car where top_speed > ?"
      )
      assert(query.frag.params == Vector(minSpeed))
      assert(result == allCars.tail)

    test("select query with aliasing"):
      val minSpeed = 210
      val cAlias = car.alias("c")
      val query =
        sql"select ${cAlias.all} from $cAlias where ${cAlias.topSpeed} > $minSpeed"
          .query[Car]
      val result =
        runIO:
          xa().connect:
            query.run()
      assertNoDiff(
        query.frag.sqlString,
        "select c.model, c.id, c.top_speed, c.vin, c.created, c.related_car_ids from car c where c.top_speed > ?"
      )
      assert(query.frag.params == Vector(minSpeed))
      assert(result == allCars.tail)

    test("select via option"):
      val vin = Option(124)
      val cars =
        runIO:
          xa().connect:
            sql"select * from car where vin = $vin"
              .query[Car]
              .run()
      assert(cars == allCars.filter(_.vinNumber == vin))

    test("reads null int as None and not Some(0)"):
      val maybeCar =
        runIO:
          xa().connect:
            carRepo.findById(3L)
      assert(maybeCar.get.vinNumber == None)

    test("created timestamps should match"):
      val allCars =
        runIO:
          xa().connect:
            carRepo.findAll
      assert(allCars.map(_.created) == allCars.map(_.created))

    test(".query iterator"):
      val carsCount =
        runIO:
          xa().connect:
            Using.Manager(implicit use =>
              val it = sql"SELECT * FROM car".query[Car].iterator()
              it.map(_.id).size
            )
      assert(carsCount == Success(3))
