package snowflake

import java.time.Instant
import java.util.Base64
import byteutil._

opaque type Snowflake = Long

object Snowflake {
    def apply(guid: Long): Snowflake = guid
}

given (snowflake: Snowflake) {
    def toHex: String = snowflake.toHexString
    def value: Long = snowflake
}

given Ordering[Snowflake] = Ordering.Long 

class SnowflakeFactory(
    epoch: Instant, 
    node: Int,
    timestampBits: Int = 42,
    sequenceBits: Int = 12,
    nodeBits: Int = 10
) {
    private[this] val epochOffset = epoch.toEpochMilli
    require(timestampBits + nodeBits + sequenceBits <= 64, s"Too many bits")
    requireInRange("node", node.toLong, nodeLimit)

    inline def bitTarget = timestampBits + sequenceBits + nodeBits
    inline def limit(bits: Int): Long = math.pow(2, bits.toDouble).toLong - 1
    inline def timestampLimit = limit(timestampBits)
    inline def nodeLimit = limit(nodeBits)
    inline def sequenceLimit = limit(sequenceBits)

    inline def timestampOffset(unixTimestamp: Long): Long =
        val timestamp = epoch.toEpochMilli - unixTimestamp
        requireInRange("timestamp", timestamp, timestampLimit)
        timestamp

    def pure(unixTimestamp: Long, sequence: Int): Snowflake =
        requireInRange("sequence", sequence.toLong, sequenceLimit)
        val timestamp = unixTimestamp - epochOffset
        requireInRange("timestamp", timestamp, timestampLimit)

        val timestampPart = timestamp << (sequenceBits + nodeBits)
        val sequencePart = sequence << nodeBits
        
        Snowflake(timestampPart | sequencePart | node)
        
    def generator: () => Snowflake =
        import scala.annotation.tailrec

        var sequence = 0
        var previousTimestamp: Long = -1

        @tailrec def spinlock(ts: Long): Long = 
            if ts == previousTimestamp 
            then spinlock(System.currentTimeMillis)
            else ts
        
        () => {
            val timestamp =
                val currentTimestamp = System.currentTimeMillis
                if currentTimestamp == previousTimestamp then
                    if sequence < sequenceLimit then
                        sequence += 1
                        currentTimestamp
                    else
                        sequence = 0
                        spinlock(System.currentTimeMillis)
                else
                    sequence = 0
                    currentTimestamp

            previousTimestamp = timestamp
            pure(timestamp, sequence)
        }
    
    private[this] val extraBytes = ((64 - bitTarget) / 8).toByte

    def toBase64(snowflake: Snowflake, encoder: Base64.Encoder): String =
        encoder.encodeToString(snowflake.value.bytes(skip = extraBytes))

    private[this] inline def requireInRange[N](name: String, value: Long, max: Long) =
        require(value >= 0 && value <= max, s"'$name' value must be between 0 and $max; was $value")
        value
}
