package array

import array.builtins.compareAPLArrays
import array.rendertext.String2D
import array.rendertext.encloseInBox
import array.rendertext.maybeWrapInParens
import array.rendertext.renderStringValue

enum class APLValueType(val typeName: String) {
    INTEGER("integer"),
    FLOAT("float"),
    COMPLEX("complex"),
    CHAR("char"),
    ARRAY("array"),
    SYMBOL("symbol"),
    LAMBDA_FN("function"),
    LIST("list"),
    MAP("map"),
    INTERNAL("internal")
}

enum class FormatStyle {
    PLAIN,
    READABLE,
    PRETTY
}

class AxisLabel(val title: String)

class DimensionLabels(val labels: List<List<AxisLabel?>?>) {
    fun computeLabelledAxes(): BooleanArray {
        val result = BooleanArray(labels.size) { i ->
            val labelList = labels[i]
            if (labelList == null) {
                false
            } else {
                var found = false
                for (element in labelList) {
                    if (element != null) {
                        found = true
                        break
                    }
                }
                found
            }
        }
        return result
    }

    companion object {
        fun computeLabelledAxis(value: APLValue): BooleanArray {
            val labels = value.labels
            return if (labels == null) {
                BooleanArray(value.dimensions.size) { false }
            } else {
                labels.computeLabelledAxes()
            }
        }

        fun makeEmpty(dimensions: Dimensions): DimensionLabels {
            val result = ArrayList<List<AxisLabel?>?>(dimensions.size)
            repeat(dimensions.size) {
                result.add(null)
            }
            return DimensionLabels(result)
        }

        fun makeDerived(dimensions: Dimensions, oldLabels: DimensionLabels?, newLabels: List<List<AxisLabel?>?>): DimensionLabels {
            assertx(newLabels.size == dimensions.size)
            val oldLabelsList = oldLabels?.labels
            val result = ArrayList<List<AxisLabel?>?>(dimensions.size)
            repeat(dimensions.size) { i ->
                val newLabelsList = newLabels[i]
                val v = when {
                    newLabelsList != null -> {
                        assertx(newLabelsList.size == dimensions[i]) { "newLabelsList does not have correct size" }
                        newLabelsList
                    }
                    oldLabelsList != null -> oldLabelsList[i]
                    else -> null
                }
                result.add(v)
            }
            return DimensionLabels(result)
        }
    }
}

enum class ArrayMemberType {
    LONG,
    DOUBLE,
    GENERIC
}

abstract class APLValue {
    abstract val aplValueType: APLValueType

    abstract val dimensions: Dimensions
    open val rank: Int get() = dimensions.size

    open val specialisedType: ArrayMemberType get() = ArrayMemberType.GENERIC

    abstract fun valueAt(p: Int): APLValue
    open fun valueAtLong(p: Int, pos: Position?) = valueAt(p).ensureNumber(pos).asLong(pos)
    open fun valueAtDouble(p: Int, pos: Position?) = valueAt(p).ensureNumber(pos).asDouble(pos)
    open fun valueAtInt(p: Int, pos: Position?): Int {
        val l = valueAtLong(p, pos)
        if (l < Int.MIN_VALUE || l > Int.MAX_VALUE) {
            throw IntMagnitudeException(l, pos)
        }
        return l.toInt()
    }

    open val size: Int get() = dimensions.contentSize()

    abstract fun formatted(style: FormatStyle = FormatStyle.PRETTY): String
    open fun formattedAsCodeRequiresParens() = true
    abstract fun collapseInt(): APLValue
    abstract fun collapseFirstLevel(): APLValue
    fun isScalar(): Boolean = rank == 0
    open fun defaultValue(): APLValue = APLLONG_0
    abstract fun arrayify(): APLValue
    open fun unwrapDeferredValue(): APLValue = this

    abstract fun compareEquals(reference: APLValue): Boolean

    open fun compare(reference: APLValue, pos: Position? = null): Int {
        fun throwError(): Nothing {
            throwAPLException(
                IncompatibleTypeException(
                    "Comparison not implemented for objects of type ${this.aplValueType.typeName} to ${reference.aplValueType.typeName}",
                    pos))
        }

        if (this::class == reference::class) {
            throw IllegalStateException("Comparison function not implemented for objects of type: ${this::class}")
        }
        val pos0 = typeToPosition[this::class] ?: throwError()
        val pos1 = typeToPosition[reference::class] ?: throwError()
        assertx(pos0 != pos1)
        return if (pos0 < pos1) -1 else 1
    }

    abstract fun disclose(): APLValue

    open val labels: DimensionLabels? get() = null

    fun collapse(): APLValue {
        val l = labels
        val v = collapseInt()
        return if (l == null) {
            v
        } else if (v === this) {
            this
        } else {
            LabelledArray(v, l)
        }
    }

    /**
     * Return a value which can be used as a hash key when storing references to this object in Kotlin maps.
     * The key must follow the standard equals/hashCode conventions with respect to the object which it
     * represents.
     *
     * In other words, if two instances of [APLValue] are to be considered equivalent, then the objects returned
     * by this method should be the same when compared using [equals] and return the same value from [hashCode].
     */
    abstract fun makeKey(): APLValueKey

    fun singleValueOrError(): APLValue {
        return when {
            rank == 0 -> this
            size == 1 -> valueAt(0)
            else -> throw IllegalStateException("Expected a single element in array, found ${size} elements")
        }
    }

    open fun ensureNumber(pos: Position? = null): APLNumber {
        val v = unwrapDeferredValue()
        if (v === this) {
            throwAPLException(IncompatibleTypeException("Value $this is not a numeric value (type=${aplValueType.typeName})", pos))
        } else {
            return v.ensureNumber(pos)
        }
    }

    open fun ensureSymbol(pos: Position? = null): APLSymbol {
        val v = unwrapDeferredValue()
        if (v === this) {
            throwAPLException(IncompatibleTypeException("Value $this is not a symbol (type=${aplValueType.typeName})", pos))
        } else {
            return v.ensureSymbol(pos)
        }
    }

    open fun ensureList(pos: Position? = null): APLList {
        val v = unwrapDeferredValue()
        if (v === this) {
            throwAPLException(IncompatibleTypeException("Value $this is not a list (type=${aplValueType.typeName})", pos))
        } else {
            return v.ensureList(pos)
        }
    }

    open fun ensureMap(pos: Position): APLMap {
        val v = unwrapDeferredValue()
        if (v === this) {
            throwAPLException(IncompatibleTypeException("Value $this is not a map (type=${aplValueType.typeName})", pos))
        } else {
            return v.ensureMap(pos)
        }
    }

    fun toIntArray(pos: Position): IntArray {
        return IntArray(size) { i -> valueAtInt(i, pos) }
    }

    fun toDoubleArray(pos: Position): DoubleArray {
        return DoubleArray(size) { i -> valueAtDouble(i, pos) }
    }

    open fun asBoolean(pos: Position? = null): Boolean {
        val v = unwrapDeferredValue()
        return if (v == this) {
            true
        } else {
            v.asBoolean(pos)
        }
    }

    companion object {
        val typeSortOrder = arrayOf(APLLong::class, APLDouble::class, APLComplex::class, APLChar::class, APLArray::class, APLSymbol::class)
        val typeToPosition = typeSortOrder.mapIndexed { i, cl -> cl to i }.toMap()
    }

    abstract class APLValueKey(val value: APLValue) {
        override fun equals(other: Any?) = other is APLValueKey && value.compareEquals(other.value)
        override fun hashCode(): Int = throw RuntimeException("Need to implement hashCode")
    }

    class APLValueKeyImpl(value: APLValue, val data: Any) : APLValueKey(value) {
        override fun equals(other: Any?) = other is APLValueKeyImpl && data == other.data
        override fun hashCode() = data.hashCode()
    }
}

fun APLValue.listify(): APLList {
    val v = unwrapDeferredValue()
    return if (v is APLList) {
        v
    } else {
        APLList(listOf(v))
    }
}

/**
 * Converts the content of this value to a byte array according to the following rules:
 *
 *   - If the array is not a scalar or vector, throw an exception
 *   - If the size of the array is zero, return a zero-length [ByteArray]
 *   - If each value in the array is an integer in the range 0-255, create a byte array with the corresponding values
 *   - If each value in the array is a character, encode the string as UTF-8
 *   - Otherwise, throw an exception
 */
fun APLValue.asByteArray(pos: Position? = null): ByteArray {
    val v = this.collapse().arrayify()
    if (v.dimensions.size != 1) {
        throwAPLException(InvalidDimensionsException("Value must be a scalar or a one-dimensional array", pos))
    }
    val size = v.dimensions[0]
    if (size == 0) {
        return byteArrayOf()
    } else {
        return when (val firstValue = v.valueAt(0)) {
            is APLNumber -> {
                ByteArray(size) { i ->
                    val valueInt = (if (i == 0) firstValue else v.valueAt(i)).ensureNumber(pos).asInt()
                    if (valueInt < 0 || valueInt > 255) {
                        throwAPLException(APLEvalException("Element at index ${i} in array is not a byte: ${valueInt}", pos))
                    }
                    valueInt.toByte()
                }
            }
            is APLChar -> {
                v.toStringValue(pos).encodeToByteArray()
            }
            else -> {
                throwAPLException(APLEvalException("Value cannot be converted to byte array", pos))
            }
        }
    }
}

inline fun APLValue.iterateMembers(fn: (APLValue) -> Unit) {
    if (rank == 0) {
        fn(this)
    } else {
        for (i in 0 until size) {
            fn(valueAt(i))
        }
    }
}

inline fun APLValue.iterateMembersWithPosition(fn: (APLValue, Int) -> Unit) {
    val v = unwrapDeferredValue()
    when {
        v is APLSingleValue -> fn(v, 0)
        v.rank == 0 -> fn(valueAt(0), 0)
        else -> (0 until v.size).forEach { i ->
            fn(valueAt(i), i)
        }
    }
}

fun APLValue.membersSequence(): Sequence<APLValue> {
    val v = unwrapDeferredValue()
    return if (v is APLSingleValue) {
        sequenceOf(v)
    } else {
        Sequence {
            val length = v.size
            var index = 0
            object : Iterator<APLValue> {
                override fun hasNext() = index < length
                override fun next() = v.valueAt(index++)
            }
        }
    }
}

abstract class APLSingleValue : APLValue() {
    override val dimensions get() = emptyDimensions()
    override fun valueAt(p: Int) =
        if (p == 0) this else throwAPLException(APLIndexOutOfBoundsException("Reading at non-zero index ${p} from scalar"))

    override val size get() = 1
    override val rank get() = 0
    override fun collapseInt() = this
    override fun collapseFirstLevel() = this
    override fun arrayify() = APLArrayImpl.make(dimensionsOfSize(1)) { this }
    override fun disclose() = this
}

abstract class APLArray : APLValue() {
    override val aplValueType: APLValueType get() = APLValueType.ARRAY

    override fun collapseInt(): APLValue {
        val v = unwrapDeferredValue()
        return when {
            v.rank == 0 -> EnclosedAPLValue.make(v.valueAt(0).collapseInt())
            else -> CollapsedArrayImpl.make(v)
        }
    }

    override fun collapseFirstLevel(): APLValue {
        return APLArrayImpl(dimensions, Array(dimensions.contentSize(), this::valueAt))
    }

    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> arrayAsString(this, FormatStyle.PLAIN)
            FormatStyle.PRETTY -> arrayAsString(this, FormatStyle.PRETTY)
            FormatStyle.READABLE -> arrayToAPLFormat(this)
        }

    override fun formattedAsCodeRequiresParens() = !isStringValue()

    override fun arrayify() = if (rank == 0) APLArrayImpl.make(dimensionsOfSize(1)) { valueAt(0) } else this

    override fun compareEquals(reference: APLValue): Boolean {
        val u = this.unwrapDeferredValue()
        if (u is APLSingleValue) {
            return u.compareEquals(reference)
        } else {
            val uRef = reference.unwrapDeferredValue()
            if (!u.dimensions.compareEquals(uRef.dimensions)) {
                return false
            }
            for (i in 0 until u.size) {
                val o1 = u.valueAt(i)
                val o2 = uRef.valueAt(i)
                if (!o1.compareEquals(o2)) {
                    return false
                }
            }
            return true
        }
    }

    override fun compare(reference: APLValue, pos: Position?): Int {
        return when {
            isScalar() && reference.isScalar() -> {
                return if (reference is APLSingleValue) {
                    -1
                } else {
                    valueAt(0).compare(reference.valueAt(0), pos)
                }
            }
            // Until we have a proper ordering of all types, we have to prevent comparing scalars to anything which is not a scalar
            isScalar() && !reference.isScalar() -> {
                throwAPLException(IncompatibleTypeException("Comparison is not supported using these types", pos))
            }
            !isScalar() && reference.isScalar() -> {
                throwAPLException(IncompatibleTypeException("Comparison is not supported using these types", pos))
            }
            else -> compareAPLArrays(this, reference)
        }
    }

    override fun disclose() = if (dimensions.size == 0) valueAt(0) else this

    override fun makeKey() = object : APLValueKey(this) {
        override fun hashCode(): Int {
            var curr = 0
            dimensions.dimensions.forEach { dim ->
                curr = (curr * 63) xor dim
            }
            membersSequence().forEach { v ->
                curr = (curr * 63) xor v.makeKey().hashCode()
            }
            return curr
        }
    }
}

class LabelledArray(val value: APLValue, override val labels: DimensionLabels) : APLArray() {
    override val dimensions = value.dimensions
    override fun valueAt(p: Int) = value.valueAt(p)

    override fun collapseInt(): APLValue {
        return value.collapseInt()
    }

    companion object {
        fun make(value: APLValue, extraLabels: List<List<AxisLabel?>?>): LabelledArray {
            return LabelledArray(value, DimensionLabels.makeDerived(value.dimensions, value.labels, extraLabels))
        }
    }
}

class APLMap(val content: ImmutableMap2<APLValueKey, APLValue>) : APLSingleValue() {
    override val aplValueType get() = APLValueType.MAP
    override val dimensions = emptyDimensions()

    override fun formatted(style: FormatStyle): String {
        return when (style) {
            FormatStyle.PLAIN -> "map[size=${content.size}]"
            FormatStyle.READABLE -> formatMapReadable()
            FormatStyle.PRETTY -> formatMapPretty()
        }
    }

    private fun formatMapReadable(): String {
        val buf = StringBuilder()
        buf.append("map ${content.size} 2⍴")
        var first = true
        content.forEach { (k, v) ->
            if (first) {
                first = false
            } else {
                buf.append(" ")
            }
            maybeWrapInParens(buf, k.value)
            buf.append(" ")
            maybeWrapInParens(buf, v)
        }
        return buf.toString()
    }

    private fun formatMapPretty(): String {
        val buf = StringBuilder()
        buf.append("map(size=${content.size})\n")
        val s = String2D(aplMapToArray().formatted(FormatStyle.PRETTY))
        repeat(s.height()) { i ->
            buf.append("  ")
            s.row(i).forEach { ch ->
                buf.append(ch)
            }
            buf.append("\n")
        }
        return buf.toString()
    }

    override fun compareEquals(reference: APLValue): Boolean {
        if (reference !is APLMap) {
            return false
        }
        if (content.size != reference.content.size) {
            return false
        }
        content.forEach { (key, value) ->
            val v = reference.content[key] ?: return false
            if (!value.compareEquals(v)) {
                return false
            }
        }
        return true
    }

    override fun makeKey(): APLValueKey {
        return APLValueKeyImpl(this, content)
    }

    override fun ensureMap(pos: Position): APLMap {
        return this
    }

    fun lookupValue(key: APLValue): APLValue {
        return content[key.makeKey()] ?: APLNullValue.APL_NULL_INSTANCE
    }

    @Suppress("unused")
    fun updateValue(key: APLValue, value: APLValue): APLMap {
        return APLMap(content.copyAndPut(key.makeKey(), value))
    }

    fun updateValues(elements: List<Pair<APLValue, APLValue>>): APLValue {
        return when (elements.size) {
            0 -> this
            1 -> elements[0].let { (key, value) -> APLMap(content.copyAndPut(key.makeKey(), value)) }
            else -> APLMap(content.copyAndPutMultiple(*elements.map { v -> Pair(v.first.makeKey(), v.second) }.toTypedArray()))
        }
    }

    fun elementCount(): Int {
        return content.size
    }

    fun removeValues(toRemove: ArrayList<APLValue>): APLMap {
        return APLMap(content.copyWithoutMultiple(toRemove.map { v -> v.makeKey() }.toTypedArray()))
    }

    fun aplMapToArray(): APLValue {
        val res = ArrayList<APLValue>()
        content.forEach { (key, value) ->
            res.add(key.value)
            res.add(value)
        }
        return APLArrayList(dimensionsOfSize(res.size / 2, 2), res)
    }
}

class APLList(val elements: List<APLValue>) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.LIST

    override val dimensions get() = emptyDimensions()

    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> elements.joinToString(separator = " ; ") { v -> v.formatted(FormatStyle.PLAIN) }
            FormatStyle.PRETTY -> elements.joinToString(separator = "\n; value\n") { v -> v.formatted(FormatStyle.PRETTY) }
            FormatStyle.READABLE -> elements.joinToString(separator = " ; ") { v -> v.formatted(FormatStyle.READABLE) }
        }

    override fun collapseInt() = this

    override fun ensureList(pos: Position?) = this

    override fun compareEquals(reference: APLValue): Boolean {
        if (reference !is APLList) {
            return false
        }
        if (elements.size != reference.elements.size) {
            return false
        }
        elements.indices.forEach { i ->
            if (!listElement(i).compareEquals(reference.listElement(i))) {
                return false
            }
        }
        return true
    }

    override fun makeKey(): APLValueKey {
        return APLValueKeyImpl(this, ComparableList<Any>().apply { addAll(elements.map(APLValue::makeKey)) })
    }

    fun listSize() = elements.size
    fun listElement(index: Int) = elements[index]

    override fun compare(reference: APLValue, pos: Position?): Int {
        return if (reference is APLList) {
            if (elements.size == reference.elements.size) {
                repeat(elements.size) { i ->
                    val res = this.elements[i].compare(reference.elements[i])
                    if (res != 0) {
                        return res
                    }
                }
                return 0
            } else {
                elements.size.compareTo(reference.elements.size)
            }
        } else {
            super.compare(reference, pos)
        }
    }
}

class ComparableList<T> : MutableList<T> by ArrayList() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComparableList<*>) return false
        if (size != other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var curr = 0
        for (i in 0 until size) {
            curr = (curr * 63) xor this[i].hashCode()
        }
        return curr
    }
}

private fun arrayToAPLFormat(value: APLArray): String {
    val v = value.collapse()
    return if (v.isStringValue()) {
        renderStringValue(v, FormatStyle.READABLE)
    } else {
        arrayToAPLFormatStandard(v as APLArray)
    }
}

private fun arrayToAPLFormatStandard(value: APLArray): String {
    val buf = StringBuilder()
    val dimensions = value.dimensions
    if (dimensions.size == 0) {
        buf.append("⊂")
        buf.append(value.valueAt(0).formatted(FormatStyle.READABLE))
    } else {
        buf.append(dimensions.dimensions.joinToString(separator = " "))
        buf.append("⍴")
        if (value.size == 0) {
            buf.append("1")
        } else {
            for (i in 0 until value.size) {
                val a = value.valueAt(i)
                if (i > 0) {
                    buf.append(" ")
                }
                maybeWrapInParens(buf, a)
            }
        }
    }
    return buf.toString()
}

fun isNullValue(value: APLValue): Boolean {
    val dimensions = value.dimensions
    return dimensions.size == 1 && dimensions[0] == 0
}

fun APLValue.isStringValue(): Boolean {
    val dimensions = this.dimensions
    if (dimensions.size == 1) {
        for (i in 0 until this.size) {
            val v = this.valueAt(i)
            if (v !is APLChar) {
                return false
            }
        }
        return true
    } else {
        return false
    }
}

fun APLValue.toStringValueOrNull(): String? {
    val dimensions = this.dimensions
    if (dimensions.size != 1) {
        return null
    }

    val buf = StringBuilder()
    for (i in 0 until this.size) {
        val v = this.valueAt(i)
        if (v !is APLChar) {
            return null
        }
        buf.append(v.asString())
    }

    return buf.toString()
}

fun APLValue.toStringValue(pos: Position? = null): String {
    return this.toStringValueOrNull() ?: throwAPLException(IncompatibleTypeException("Argument is not a string", pos))
}

fun arrayAsString(array: APLValue, style: FormatStyle): String {
    val v = array.collapse() // This is to prevent multiple evaluations during printing
    return encloseInBox(v, style)
}

class ConstantArray(
    override val dimensions: Dimensions,
    value: APLValue
) : APLArray() {

    private val valueInternal = value.unwrapDeferredValue()

    override val specialisedType = when (valueInternal) {
        is APLLong -> ArrayMemberType.LONG
        is APLDouble -> ArrayMemberType.DOUBLE
        else -> ArrayMemberType.GENERIC
    }

    override fun valueAt(p: Int) = valueInternal
}

open class APLArrayImpl(
    override val dimensions: Dimensions,
    private val values: Array<APLValue>
) : APLArray() {

    override fun valueAt(p: Int) = values[p]
    override fun toString() = "APLArrayImpl[${dimensions}, ${Arrays.toString(values)}]"

    companion object {
        inline fun make(dimensions: Dimensions, fn: (index: Int) -> APLValue): APLArrayImpl {
            val content = Array(dimensions.contentSize()) { index -> fn(index) }
            return APLArrayImpl(dimensions, content)
        }
    }
}

class CollapsedArrayImpl(dimensions: Dimensions, values: Array<APLValue>) : APLArrayImpl(dimensions, values) {
    override fun collapseInt() = this

    companion object {
        fun make(orig: APLValue): APLValue {
            val d = orig.dimensions
            return when (orig.specialisedType) {
                ArrayMemberType.LONG -> APLArrayLong(d, LongArray(d.contentSize()) { index -> orig.valueAtLong(index, null) })
                ArrayMemberType.DOUBLE -> APLArrayDouble(d, DoubleArray(d.contentSize()) { index -> orig.valueAtDouble(index, null) })
                ArrayMemberType.GENERIC -> {
                    val content = Array(d.contentSize()) { index -> orig.valueAt(index).collapseInt() }
                    CollapsedArrayImpl(d, content)
                }
            }
        }
    }
}

class APLArrayList(override val dimensions: Dimensions, private val values: List<APLValue>) : APLArray() {
    override fun valueAt(p: Int) = values[p]
    override fun toString() = values.toString()
}

class EnclosedAPLValue private constructor(val value: APLValue) : APLArray() {
    override val dimensions: Dimensions
        get() = emptyDimensions()

    override fun valueAt(p: Int): APLValue {
        if (p != 0) {
            throwAPLException(APLIndexOutOfBoundsException("Attempt to read from a non-zero index "))
        }
        return value
    }

    override fun disclose() = value

    companion object {
        fun make(value: APLValue): APLValue {
            return if (value is APLSingleValue) {
                value
            } else {
                EnclosedAPLValue(value)
            }
        }
    }
}

class APLChar(val value: Int) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.CHAR
    fun asString() = charToString(value)
    override fun formatted(style: FormatStyle) = when (style) {
        FormatStyle.PLAIN -> charToString(value)
        FormatStyle.PRETTY -> "@${charToString(value)}"
        FormatStyle.READABLE -> "@${charToString(value)}"
    }

    override fun formattedAsCodeRequiresParens() = false

    override fun compareEquals(reference: APLValue) = reference is APLChar && value == reference.value

    override fun compare(reference: APLValue, pos: Position?): Int {
        return if (reference is APLChar) {
            value.compareTo(reference.value)
        } else {
            super.compare(reference, pos)
        }
    }

    override fun toString() = "APLChar['${asString()}' 0x${value.toString(16)}]"

    override fun makeKey() = APLValueKeyImpl(this, value)
}

class APLString(val content: IntArray) : APLArray() {
    constructor(string: String) : this(string.asCodepointList().toIntArray())

    override val dimensions = dimensionsOfSize(content.size)
    override fun valueAt(p: Int) = APLChar(content[p])

    override fun collapseInt() = this

    override fun toString() = "APLString[value=\"${content.joinToString(transform = ::charToString)}\"]"

    companion object {
        fun make(s: String) = APLString(s)
    }
}

class APLNullValue private constructor() : APLArray() {
    override val dimensions get() = nullDimensions()
    override fun valueAt(p: Int) = throwAPLException(APLIndexOutOfBoundsException("Attempt to read a value from the null value"))

    companion object {
        val APL_NULL_INSTANCE = APLNullValue()
    }
}

/**
 * Special version of of the regular null value that is emitted by a blank expression.
 * This value acts like a regular null value in most cases. However, in certain contexts
 * it has different behaviour. The main case is for array indexing.
 */
class APLEmpty : APLArray() {
    override val dimensions get() = nullDimensions()
    override fun valueAt(p: Int) = throwAPLException(APLIndexOutOfBoundsException("Attempt to read a value from the null value"))
}

abstract class DeferredResultArray : APLArray() {
    override fun unwrapDeferredValue(): APLValue {
        return if (dimensions.isEmpty()) valueAt(0).unwrapDeferredValue() else this
    }
}

class APLSymbol(val value: Symbol) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.SYMBOL
    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> "${value.namespace.name}:${value.symbolName}"
            FormatStyle.PRETTY -> "${value.namespace.name}:${value.symbolName}"
            FormatStyle.READABLE -> "'${value.namespace.name}:${value.symbolName}"
        }

    override fun compareEquals(reference: APLValue) = reference is APLSymbol && value == reference.value

    override fun compare(reference: APLValue, pos: Position?): Int {
        return if (reference is APLSymbol) {
            value.compareTo(reference.value)
        } else {
            super.compare(reference, pos)
        }
    }

    override fun ensureSymbol(pos: Position?) = this

    override fun makeKey() = APLValueKeyImpl(this, value)
}

/**
 * This class represents a closure. It wraps a function and a context to use when calling the closure.
 *
 * @param fn the function that is wrapped by the closure
 * @param previousContext the context to use when calling the function
 */
class LambdaValue(private val fn: APLFunction, private val previousContext: RuntimeContext) : APLSingleValue() {
    override val aplValueType: APLValueType get() = APLValueType.LAMBDA_FN
    override fun formatted(style: FormatStyle) =
        when (style) {
            FormatStyle.PLAIN -> "function"
            FormatStyle.READABLE -> throw IllegalArgumentException("Functions can't be printed in readable form")
            FormatStyle.PRETTY -> "function"
        }

    override fun compareEquals(reference: APLValue) = this === reference

    override fun makeKey() = APLValueKeyImpl(this, fn)

    fun makeClosure(): APLFunction {
        return object : APLFunction(fn.pos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                return fn.eval1Arg(previousContext, a, axis)
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                return fn.eval2Arg(previousContext, a, b, axis)
            }

            override fun identityValue() = fn.identityValue()
        }
    }
}

class IntArrayValue(
    srcDimensions: Dimensions,
    val values: IntArray
) : APLArray() {

    override val specialisedType get() = ArrayMemberType.LONG
    override val dimensions = srcDimensions
    override fun valueAt(p: Int) = values[p].makeAPLNumber()
    override fun valueAtLong(p: Int, pos: Position?) = values[p].toLong()

    fun intValueAt(p: Int) = values[p]

    companion object {
        fun fromAPLValue(src: APLValue, pos: Position? = null): IntArrayValue {
            return if (src is IntArrayValue) {
                src
            } else {
                val dimensions = src.dimensions
                val values = IntArray(dimensions.contentSize()) { i -> src.valueAtInt(i, pos) }
                IntArrayValue(dimensions, values)
            }
        }
    }
}

class APLArrayLong(
    override val dimensions: Dimensions,
    val values: LongArray
) : APLArray() {
    override val specialisedType get() = ArrayMemberType.LONG
    override fun valueAt(p: Int) = values[p].makeAPLNumber()
    override fun valueAtLong(p: Int, pos: Position?) = values[p]
    override fun collapseInt() = this
}

class APLArrayDouble(
    override val dimensions: Dimensions,
    val values: DoubleArray
) : APLArray() {
    override val specialisedType get() = ArrayMemberType.DOUBLE
    override fun valueAt(p: Int) = values[p].makeAPLNumber()
    override fun valueAtDouble(p: Int, pos: Position?) = values[p]
    override fun collapseInt() = this
}

open class DelegatedValue(val value: APLValue) : APLValue() {
    override val aplValueType: APLValueType get() = value.aplValueType
    override val dimensions: Dimensions get() = value.dimensions
    override val rank: Int get() = value.rank
    override val specialisedType: ArrayMemberType get() = value.specialisedType
    override fun valueAt(p: Int) = value.valueAt(p)
    override fun valueAtLong(p: Int, pos: Position?) = value.valueAtLong(p, pos)
    override fun valueAtDouble(p: Int, pos: Position?) = value.valueAtDouble(p, pos)
    override fun valueAtInt(p: Int, pos: Position?) = value.valueAtInt(p, pos)
    override val size: Int get() = value.size
    override fun formatted(style: FormatStyle) = value.formatted(style)
    override fun collapseInt() = value.collapseInt()
    override fun collapseFirstLevel() = value.collapseFirstLevel()
    override fun defaultValue() = value.defaultValue()
    override fun arrayify() = value.arrayify()
    override fun unwrapDeferredValue() = value.unwrapDeferredValue()
    override fun compareEquals(reference: APLValue) = value.compareEquals(reference)
    override fun compare(reference: APLValue, pos: Position?) = value.compare(reference, pos)
    override fun disclose() = value.disclose()
    override val labels: DimensionLabels? get() = value.labels
    override fun makeKey() = value.makeKey()
    override fun ensureSymbol(pos: Position?) = value.ensureSymbol(pos)
    override fun ensureList(pos: Position?) = value.ensureList(pos)
    override fun ensureMap(pos: Position) = value.ensureMap(pos)
    override fun asBoolean(pos: Position?) = value.asBoolean(pos)
}
