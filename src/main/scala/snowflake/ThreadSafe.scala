package snowflake.threadsafe

def (f: () => A) threadsafe[A]: () => A =
    val lock = new {}
    () => lock.synchronized(f())
