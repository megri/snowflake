package snowflake

import threadsafe._

import java.time.Instant

val factoryEpoch = Instant.parse("2019-01-01T00:00:00Z")
val factoryEpochTimestamp = factoryEpoch.toEpochMilli

val factory = SnowflakeFactory(
    epoch = factoryEpoch,
    timestampBits = 42,
    sequenceBits = 12,
    nodeBits = 10,
    node = 0
)

val amount = 10000

def run(): Unit =
    val generateSnowflake: () => Snowflake = factory.generator

    report {
        Iterator.continually(generateSnowflake()).take(amount).toList
    }

@main def runAsync(): Unit =
    import scala.concurrent._
    import scala.concurrent.duration._
    import java.util.concurrent.Executors

    val generateSnowflake: () => Snowflake = factory.generator.threadsafe

    val executorService = Executors.newFixedThreadPool(10)
    given ec: ExecutionContext = ExecutionContext.fromExecutor(executorService)
    def asyncSnowflake(): Future[Snowflake] = Future(generateSnowflake())

    report {
        Await.result(
            Future.sequence(for _ <- 1 to amount yield asyncSnowflake()),
            atMost = 3.seconds
        )
    }

    executorService.shutdown()

def report(block: => Seq[Snowflake]): Unit =
    import java.time.Duration
    import java.util.Base64

    val startTime = System.nanoTime
    val snowflakes = block
    val endTime = System.nanoTime
    val duration = Duration.ofNanos(endTime - startTime).toMillis

    // res should be empty
    val res = snowflakes.map(factory.toBase64(_, Base64.getUrlEncoder.withoutPadding))
    val duplicates = res.map(item => item -> res.count(_ == item)).filter(_._2 > 1)
    val duplicatesCount = duplicates.size
    
    println(s"generated: ${snowflakes.size} snowflakes")
    println(s"took: $duration ms")
    println(s"Duplicates: $duplicatesCount")
