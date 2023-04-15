package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class BitwiseTest {

    //////////////////
    // and
    //////////////////

    @Test
    fun andBigIntWithBigInt() {
        val a = BigInt.of(0b0011)
        val b = BigInt.of(0b1010)
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andLongWithBigInt() {
        val a = 0b0011.toLong()
        val b = BigInt.of(0b1010)
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andBigIntWithLong() {
        val a = BigInt.of(0b0011)
        val b = 0b1010.toLong()
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andIntWithBigInt() {
        val a = 0b0011
        val b = BigInt.of(0b1010)
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andBigIntWithInt() {
        val a = BigInt.of(0b0011)
        val b = 0b1010
        val c = a and b
        assertEquals("2", c.toString())
    }

    //////////////////
    // or
    //////////////////

    @Test
    fun orBigIntWithBigInt() {
        val a = BigInt.of(0b0011)
        val b = BigInt.of(0b1010)
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orLongWithBigInt() {
        val a = 0b0011.toLong()
        val b = BigInt.of(0b1010)
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orBigIntWithLong() {
        val a = BigInt.of(0b0011)
        val b = 0b1010.toLong()
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orIntWithBigInt() {
        val a = 0b0011
        val b = BigInt.of(0b1010)
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orBigIntWithInt() {
        val a = BigInt.of(0b0011)
        val b = 0b1010
        val c = a or b
        assertEquals("11", c.toString())
    }

    //////////////////
    // xor
    //////////////////

    @Test
    fun xorBigIntWithBigInt() {
        val a = BigInt.of(0b0011)
        val b = BigInt.of(0b1010)
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorLongWithBigInt() {
        val a = 0b0011.toLong()
        val b = BigInt.of(0b1010)
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorBigIntWithLong() {
        val a = BigInt.of(0b0011)
        val b = 0b1010.toLong()
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorIntWithBigInt() {
        val a = 0b0011
        val b = BigInt.of(0b1010)
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorBigIntWithInt() {
        val a = BigInt.of(0b0011)
        val b = 0b1010
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun testNot() {
        val a = BigInt.of(0b0011)
        val b = a.inv()
        assertEquals("-4", b.toString())
    }

    //////////////////
    // shl
    //////////////////

    @Test
    fun shl() {
        val a = BigInt.of(0b1110001)
        val b = a shl 3
        assertEquals((0b1110001 shl 3).toString(), b.toString())
    }

    @Test
    fun shlNegativeValue() {
        val a = BigInt.of(-39)
        val b = a shl 3
        assertEquals((-39 shl 3).toString(), b.toString())
    }

    @Test
    fun shlNegativeValueNegativeSteps() {
        val a = BigInt.of(-39)
        val b = a shl -3
        assertEquals("-5", b.toString())
    }

    @Test
    fun shlNegativeSteps() {
        val a = BigInt.of(0b1110001)
        val b = a shl -3
        assertEquals("14", b.toString())
    }

    @Test
    fun shlZeroBits() {
        val a = BigInt.of(0b1110001)
        val b = a shl 0
        assertEquals(0b1110001.toString(), b.toString())
    }

    @Test
    fun largeLeftShift() {
        val a = BigInt.of(1)
        val b = a shl 4800
        val expected = """
            87898039204788324925783294151808719402282887071042527340109818285189479347
            47628073880029443343136789403580799075524480981827202376496138130502225082
            23574706530539071763950360130867942116783479927108914736606395645317528367
            98248860539589278934330893390996149386567641287056231340459740904172065305
            97184581579938901366200594707631548583000233908025482393167224204994566321
            49012689594028019551673277830146231543650507946020049674869249196972689348
            45649227052238071569186822353545103118275855411156180259555739855174253896
            90033248387741160920599663409113415839425510049833482733494620287059793335
            58030933791675108222351206969935743974688504299864962853066454845781474522
            67923641194990104371583104636690257284887683043911170630987561301779478101
            72617182225643643500926123523460382582793564341016340159319712508389026114
            64190522299930083084294155025438004534944650915863227042065004416945721572
            06809627749201455691550107728035193825784189677789643070587902791756930655
            17734120630860279396582146315115407683214433630813937782472403977712611434
            62157607282394739578835591917833371001276726492090192077541990043063972559
            44961856663670548703629320409560225932384478576976210497889755369549845504
            70110200212666677069734789857611682350713617858276021223852108315982120373
            75318113533850569318350841287614466546060308150347428261635164938202025221
            49157210943344211549624991974238173452620633250503973226046668369517253720
            964343419863308669488566515865407717376
        """.trimIndent().filter { it != '\n' }
        assertEquals(expected, b.toString())
    }

    //////////////////
    // shr
    //////////////////

    @Test
    fun shr() {
        val a = BigInt.of(0b1110001)
        val b = a shr 3
        assertEquals((0b1110001 shr 3).toString(), b.toString())
    }

    @Test
    fun shrLarge() {
        val a = BigInt.of("1" + "0".repeat(1000))
        val b = a shr 2200
        val expected = """
            54201279553584682216567238102120217362127526907193141330946366880462522850
            13048048897834137181848058871062309394148875955242318423020696890774615530
            22673134742521323748263153784641215410452341032799488902816454067958194814
            20936536472751370963944355291938970184915480192010296525291316250067187446
            797000572534086148940592265548298697658922
        """.trimIndent().filter { it != '\n' }
        assertEquals(expected, b.toString())
    }
}
