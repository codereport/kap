package array.complex

import kotlin.math.*

data class Complex(val real: Double, val imaginary: Double) {

    constructor(value: Double) : this(value, 0.0)

    fun reciprocal(): Complex {
        val scale = (real * real) + (imaginary * imaginary)
        return Complex(real / scale, -imaginary / scale)
    }

    fun abs(): Double = hypot(real, imaginary)

    operator fun unaryMinus(): Complex = Complex(-real, -imaginary)
    operator fun plus(other: Double): Complex = Complex(real + other, imaginary)
    operator fun minus(other: Double): Complex = Complex(real - other, imaginary)
    operator fun times(other: Double): Complex = Complex(real * other, imaginary * other)
    operator fun div(other: Double): Complex = Complex(real / other, imaginary / other)

    operator fun plus(other: Complex): Complex =
        Complex(real + other.real, imaginary + other.imaginary)

    operator fun minus(other: Complex): Complex =
        Complex(real - other.real, imaginary - other.imaginary)

    operator fun times(other: Complex): Complex =
        Complex(
            (real * other.real) - (imaginary * other.imaginary),
            (real * other.imaginary) + (imaginary * other.real))

    operator fun div(other: Complex): Complex = this * other.reciprocal()

    fun pow(complex: Complex): Complex {
        val arg = atan2(this.imaginary, this.real)
        val resultAbsolute = exp(ln(this.abs()) * complex.real - (arg * complex.imaginary))
        val resultArg = ln(this.abs()) * complex.imaginary + arg * complex.real
        return fromPolarCoord(resultAbsolute, resultArg)
    }

    fun signum(): Complex {
        return if (real == 0.0 && imaginary == 0.0) {
            ZERO
        } else {
            this / this.abs()
        }
    }

    fun ln(): Complex = Complex(ln(hypot(real, imaginary)), atan2(imaginary, real))
    fun log(base: Complex): Complex = ln() / base.ln()
    fun log(base: Double): Complex = log(base.toComplex())

    fun nearestGaussian(): Complex {
        return Complex(round(real), round(imaginary))
    }

    override fun equals(other: Any?) = other != null && other is Complex && real == other.real && imaginary == other.imaginary
    override fun hashCode() = real.hashCode() xor imaginary.hashCode()

    companion object {
        fun fromPolarCoord(absolute: Double, arg: Double): Complex {
            return Complex(cos(arg) * absolute, sin(arg) * absolute)
        }

        val ZERO = Complex(0.0, 0.0)
        val I = Complex(0.0, 1.0)
    }
}

operator fun Double.plus(complex: Complex) = this.toComplex() + complex
operator fun Double.times(complex: Complex) = this.toComplex() * complex
operator fun Double.minus(complex: Complex) = this.toComplex() - complex
operator fun Double.div(complex: Complex) = this.toComplex() / complex

fun Double.toComplex() = Complex(this, 0.0)
fun Double.pow(complex: Complex) = this.toComplex().pow(complex)
fun Double.log(base: Complex) = this.toComplex().log(base)

fun complexSin(v: Complex): Complex {
    return (E.pow(v * Complex.I) - E.pow(-v * Complex.I)) / Complex(0.0, 2.0)
}

fun complexCos(v: Complex): Complex {
    return (E.pow(v * Complex.I) + E.pow(-v * Complex.I)) / 2.0
}

fun complexTan(v: Complex): Complex {
    val re2 = v.real * 2
    val im2 = v.imaginary * 2
    val d = cos(re2) + cosh(im2)
    return Complex(sin(re2) / d, sinh(im2) / d)
}
