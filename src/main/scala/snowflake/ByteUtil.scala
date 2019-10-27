package snowflake.byteutil

def (long: Long) bytes(skip: Byte): Array[Byte] =
    inline def shift(steps: Int): Byte = (long >> steps & 0xff).toByte
    var remaining = 8 - skip
    val result = new Array[Byte](remaining)
    var s = 0
    
    while remaining > 0 do
        result(remaining-1) = shift(s)
        s += 8
        remaining -= 1

    result
