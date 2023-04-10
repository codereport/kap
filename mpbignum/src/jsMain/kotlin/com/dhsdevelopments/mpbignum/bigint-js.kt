package com.dhsdevelopments.mpbignum

class BigIntWrapper(val value: dynamic) {
    override fun equals(other: Any?): Boolean {
        if (other !is BigIntWrapper) {
            return false
        }

        @Suppress("UNUSED_VARIABLE")
        val a = value

        @Suppress("UNUSED_VARIABLE")
        val b = other.value
        return js("a==b") as Boolean
    }

    override fun hashCode(): Int {
        @Suppress("UNUSED_VARIABLE")
        val a = value
        val s = js("BigInt.asIntN(32,a).toString()") as String
        return s.toInt()
    }
}

actual value class BigInt(val impl: Any) {
    override fun toString(): String = inner.toString()

    internal val inner: dynamic
        get() {
            return (impl as BigIntWrapper).value
        }

    actual companion object {
        fun makeFromJs(v: dynamic): BigInt {
            return BigInt(BigIntWrapper(v))
        }
    }
}

actual val BigInt.absoluteValue: BigInt
    get() {
        @Suppress("UNUSED_VARIABLE")
        val a = this.inner
        return BigInt.makeFromJs(js("(function(){if(a<0){return -a;} else {return a;}})()"))
    }

actual operator fun BigInt.plus(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a+b"))
}

actual operator fun BigInt.minus(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a-b"))
}

actual operator fun BigInt.times(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a*b"))
}

actual operator fun BigInt.div(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a/b"))
}

actual operator fun BigInt.unaryMinus(): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner
    return BigInt.makeFromJs(js("-a"))
}

actual fun BigInt.pow(other: Long): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.toString()

    @Suppress("UNUSED_VARIABLE")
    val b = other.toString()
    return BigInt.makeFromJs(js("(function(a0,b0){return eval(\"a0**b0\");})(BigInt(a),BigInt(b))"))
}

actual operator fun BigInt.rem(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a%b"))
}

actual operator fun BigInt.compareTo(other: BigInt): Int {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return js("(function(a0,b0){if(a0<b0){return -1;} else if(a0>b0){return 1;} else {return 0;}})(a,b)")
}

actual fun BigInt.Companion.of(value: Short): BigInt {
    val stringified = value.toString()
    return BigInt.of(stringified)
}

actual fun BigInt.Companion.of(value: Int): BigInt {
    val stringified = value.toString()
    return BigInt.of(stringified)
}

actual fun BigInt.Companion.of(value: Long): BigInt {
    val stringified: String = value.toString()
    return BigInt.of(stringified)
}

actual fun BigInt.Companion.of(s: String): BigInt {
    val regex = "^-?[0-9]+$".toRegex()
    if (!regex.matches(s)) {
        throw NumberFormatException("Invalid decimal value: ${s}")
    }

    @Suppress("UNUSED_VARIABLE")
    val stringified = s
    return makeFromJs(js("BigInt(stringified)"))
}

actual infix fun BigInt.and(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a&b"))
}

actual infix fun BigInt.or(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a|b"))
}

actual infix fun BigInt.xor(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a^b"))
}

actual infix fun BigInt.shl(other: Long): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other

    return BigInt.makeFromJs(js("(function(b0){return a<<b0})(BigInt(b))"))
}

actual infix fun BigInt.shr(other: Long): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other

    return BigInt.makeFromJs(js("(function(b0){return a>>b0})(BigInt(b))"))
}

actual fun BigInt.toLong(): Long {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner
    val s = js("BigInt.asIntN(64,a).toString()") as String
    return s.toLong()
}

actual fun BigInt.toDouble(): Double {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner
    return js("Number(a)") as Double
}

actual fun BigInt.signum(): Int {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner
    return js("(function(a0){if(a0<0){return -1;} else if(a0>0){return 1;} else {return 0;}})(a)")
}

actual fun BigInt.gcd(other: BigInt): BigInt {
    return standardGcd(this, other)
}
