package array

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("ComplexRedundantLet")
class ReduceTest : APLTest() {
    @Test
    fun reduceIotaTest() {
        val result = parseAPLExpression("+/⍳1000")
        assertSimpleNumber(499500, result)
    }

    @Test
    fun reduceTestWithFunction() {
        val result = parseAPLExpression("+/⍳1+2")
        assertSimpleNumber(3, result)
    }

    @Test
    fun reduceWithSingleValue() {
        val result = parseAPLExpression("+/,4")
        assertSimpleNumber(4, result)
    }

    @Test
    fun reduceWithScalar() {
        val result = parseAPLExpression("+/4")
        assertSimpleNumber(4, result)
    }

    @Test
    fun reduceWithEmptyArg() {
        reduceTestWithFunctionName("+", 0)
        reduceTestWithFunctionName("-", 0)
        reduceTestWithFunctionName("×", 1)
        reduceTestWithFunctionName("÷", 1)
        reduceTestWithFunctionName("⋆", 1)
        reduceTestWithFunctionName("=", 1)
        reduceTestWithFunctionName("≠", 0)
        reduceTestWithFunctionName("<", 0)
        reduceTestWithFunctionName(">", 0)
        reduceTestWithFunctionName("≤", 1)
        reduceTestWithFunctionName("≥", 1)
    }

    private fun reduceTestWithFunctionName(aplFn: String, correctRes: Int) {
        val result = parseAPLExpression("${aplFn}/0⍴4")
        assertTrue(result.dimensions.compareEquals(emptyDimensions()))
        assertSimpleNumber(correctRes.toLong(), result)
    }

    @Test
    fun reduceWithEqualsAndOptimisedLong() {
        assertSimpleNumber(0, parseAPLExpression("=/⍳5"))
    }

    @Test
    fun reduceWithNonScalarCells() {
        val result = parseAPLExpression("+/ (1 2 3 4) (6 7 8 9)")
        assertDimension(emptyDimensions(), result)

        val v = result.valueAt(0)
        assertDimension(dimensionsOfSize(4), v)
        assertArrayContent(arrayOf(7, 9, 11, 13), v)
    }

    @Test
    fun reduceCustomFn() {
        val result = parseAPLExpression("{⍺+⍵+10}/⍳6")
        assertSimpleNumber(65, result)
    }

    @Test
    fun reduceTestCustomInt() {
        val result = parseAPLExpression("{⍺+⍵+10}/int:ensureLong ⍳6")
        assertSimpleNumber(65, result)
    }

    @Test
    fun reduceTestCustomNoSpecialisation() {
        val result = parseAPLExpression("{⍺+⍵+10}/int:ensureGeneric ⍳6")
        assertSimpleNumber(65, result)
    }

    @Test
    fun reduceTestCustomFnWithDoubleDefault() {
        val result = parseAPLExpression("{⍺+⍵+10}/0.0 0.1 0.2 0.3 0.4 0.5")
        assertDoubleWithRange(Pair(51.4, 51.6), result)
    }

    @Test
    fun reduceTestCustomWithDoubleGeneric() {
        val result = parseAPLExpression("{⍺+⍵+10}/int:ensureGeneric 0.0 0.1 0.2 0.3 0.4 0.5")
        assertDoubleWithRange(Pair(51.4, 51.6), result)
    }

    @Test
    fun reduceTestCustomWithSpecialisedDouble() {
        val result = parseAPLExpression("{⍺+⍵+10}/int:ensureDouble 0.0 0.1 0.2 0.3 0.4 0.5")
        assertDoubleWithRange(Pair(51.4, 51.6), result)
    }

    @Test
    fun reduceAlongAxis() {
        parseAPLExpression("e←3 4 ⍴ 1 2 3 4 5 6 7 8 9 10 11 12 ◊ +/[0] e").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(15, 18, 21, 24), result)
        }
    }

    @Test
    fun invalidAxisTest() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("+/[2] 2 3 ⍴ ⍳6")
        }
    }

    @Test
    fun multiDimensionalReduce() {
        parseAPLExpression("+/[0] 3 3 4 5 6 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(3, 4, 5, 6), result)
            assertArrayContent(
                arrayOf(
                    1080, 1083, 1086, 1089, 1092, 1095, 1098, 1101, 1104, 1107, 1110,
                    1113, 1116, 1119, 1122, 1125, 1128, 1131, 1134, 1137, 1140, 1143,
                    1146, 1149, 1152, 1155, 1158, 1161, 1164, 1167, 1170, 1173, 1176,
                    1179, 1182, 1185, 1188, 1191, 1194, 1197, 1200, 1203, 1206, 1209,
                    1212, 1215, 1218, 1221, 1224, 1227, 1230, 1233, 1236, 1239, 1242,
                    1245, 1248, 1251, 1254, 1257, 1260, 1263, 1266, 1269, 1272, 1275,
                    1278, 1281, 1284, 1287, 1290, 1293, 1296, 1299, 1302, 1305, 1308,
                    1311, 1314, 1317, 1320, 1323, 1326, 1329, 1332, 1335, 1338, 1341,
                    1344, 1347, 1350, 1353, 1356, 1359, 1362, 1365, 1368, 1371, 1374,
                    1377, 1380, 1383, 1386, 1389, 1392, 1395, 1398, 1401, 1404, 1407,
                    1410, 1413, 1416, 1419, 1422, 1425, 1428, 1431, 1434, 1437, 1440,
                    1443, 1446, 1449, 1452, 1455, 1458, 1461, 1464, 1467, 1470, 1473,
                    1476, 1479, 1482, 1485, 1488, 1491, 1494, 1497, 1500, 1503, 1506,
                    1509, 1512, 1515, 1518, 1521, 1524, 1527, 1530, 1533, 1536, 1539,
                    1542, 1545, 1548, 1551, 1554, 1557, 1560, 1563, 1566, 1569, 1572,
                    1575, 1578, 1581, 1584, 1587, 1590, 1593, 1596, 1599, 1602, 1605,
                    1608, 1611, 1614, 1617, 1620, 1623, 1626, 1629, 1632, 1635, 1638,
                    1641, 1644, 1647, 1650, 1653, 1656, 1659, 1662, 1665, 1668, 1671,
                    1674, 1677, 1680, 1683, 1686, 1689, 1692, 1695, 1698, 1701, 1704,
                    1707, 1710, 1713, 1716, 1719, 1722, 1725, 1728, 1731, 1734, 1737,
                    1740, 1743, 1746, 1749, 1752, 1755, 1758, 1761, 1764, 1767, 1770,
                    1773, 1776, 1779, 1782, 1785, 1788, 1791, 1794, 1797, 1800, 1803,
                    1806, 1809, 1812, 1815, 1818, 1821, 1824, 1827, 1830, 1833, 1836,
                    1839, 1842, 1845, 1848, 1851, 1854, 1857, 1860, 1863, 1866, 1869,
                    1872, 1875, 1878, 1881, 1884, 1887, 1890, 1893, 1896, 1899, 1902,
                    1905, 1908, 1911, 1914, 1917, 920, 923, 926, 929, 932, 935, 938, 941,
                    944, 947, 950, 953, 956, 959, 962, 965, 968, 971, 974, 977, 980, 983,
                    986, 989, 992, 995, 998, 1001, 1004, 1007, 1010, 1013, 1016, 1019,
                    1022, 1025, 1028, 1031, 1034, 1037, 1040, 1043, 1046, 1049, 1052,
                    1055, 1058, 1061, 1064, 1067, 1070, 1073, 1076, 1079, 1082, 1085,
                    1088, 1091, 1094, 1097, 1100, 1103, 1106, 1109, 1112, 1115, 1118,
                    1121, 1124, 1127, 1130, 1133, 1136, 1139, 1142, 1145, 1148, 1151,
                    1154, 1157
                ), result)
        }

        parseAPLExpression("+/[3] 3 3 4 5 6 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(3, 3, 4, 6), result)
            assertArrayContent(
                arrayOf(
                    60, 65, 70, 75, 80, 85, 210, 215, 220, 225, 230, 235, 360, 365, 370,
                    375, 380, 385, 510, 515, 520, 525, 530, 535, 660, 665, 670, 675, 680,
                    685, 810, 815, 820, 825, 830, 835, 960, 965, 970, 975, 980, 985, 1110,
                    1115, 1120, 1125, 1130, 1135, 1260, 1265, 1270, 1275, 1280, 1285,
                    1410, 1415, 1420, 1425, 1430, 1435, 1560, 1565, 1570, 1575, 1580,
                    1585, 1710, 1715, 1720, 1725, 1730, 1735, 1860, 1865, 1870, 1875,
                    1880, 1885, 2010, 2015, 2020, 2025, 2030, 2035, 2160, 2165, 2170,
                    2175, 2180, 2185, 2310, 2315, 2320, 2325, 2330, 2335, 2460, 2465,
                    2470, 2475, 2480, 2485, 2610, 2615, 2620, 2625, 2630, 2635, 2760,
                    2765, 2770, 2775, 2780, 2785, 2910, 2915, 2920, 2925, 2930, 2935,
                    3060, 3065, 3070, 3075, 3080, 3085, 3210, 3215, 3220, 3225, 3230,
                    3235, 3360, 3365, 3370, 3375, 3380, 3385, 3510, 3515, 3520, 3525,
                    3530, 3535, 3660, 3665, 3670, 3675, 3680, 3685, 3810, 3815, 3820,
                    3825, 3830, 3835, 3960, 3965, 3970, 3975, 3980, 3985, 4110, 4115,
                    4120, 4125, 4130, 4135, 4260, 4265, 4270, 4275, 4280, 4285, 4410,
                    4415, 4420, 4425, 4430, 4435, 4560, 4565, 4570, 4575, 4580, 4585,
                    4710, 4715, 4720, 4725, 4730, 4735, 4860, 4865, 4870, 4875, 4880,
                    4885, 2010, 2015, 2020, 2025, 1030, 1035, 160, 165, 170, 175, 180,
                    185, 310, 315, 320, 325, 330, 335
                ), result)
        }
    }

    @Test
    fun reduceFirstAxis() {
        parseAPLExpression("+⌿ 2 3 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(3, 5, 7), result)
        }
    }

    @Test
    fun reduceFirstAxisWithGivenAxis() {
        parseAPLExpression("+⌿[1] 2 3 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(3, 12), result)
        }
    }

    @Test
    fun plainNWise() {
        parseAPLExpression("2+/⍳10").let { result ->
            assertDimension(dimensionsOfSize(9), result)
            assertArrayContent(arrayOf(1, 3, 5, 7, 9, 11, 13, 15, 17), result)
        }
    }

    @Test
    fun plainNWiseSubarraySize3() {
        parseAPLExpression("3+/⍳10").let { result ->
            assertDimension(dimensionsOfSize(8), result)
            assertArrayContent(arrayOf(3, 6, 9, 12, 15, 18, 21, 24), result)
        }
    }

    @Test
    fun scalarWithNWise1Dimension() {
        parseAPLExpression("1+/1").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(1), result)
        }
    }

    @Test
    fun scalarWithNWise2Dimension() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("2+/1")
        }
    }

    @Test
    fun plainNWiseWithNegativeSubarraySize() {
        parseAPLExpression("¯2,/⍳10").let { result ->
            fun assertCell(index: Int, v0: Int, v1: Int) {
                val cell = result.valueAt(index)
                assertDimension(dimensionsOfSize(2), cell)
                assertArrayContent(arrayOf(v0, v1), cell)
            }
            assertDimension(dimensionsOfSize(9), result)
            assertCell(0, 1, 0)
            assertCell(1, 2, 1)
            assertCell(2, 3, 2)
            assertCell(3, 4, 3)
            assertCell(4, 5, 4)
            assertCell(5, 6, 5)
            assertCell(6, 7, 6)
            assertCell(7, 8, 7)
            assertCell(8, 9, 8)
        }
    }

    @Test
    fun multidimensionalNWiseWithCatenate() {
        parseAPLExpression("2,/3 5 ⍴ ⍳10").let { result ->
            fun assertCell(index: Int, v0: Int, v1: Int) {
                val cell = result.valueAt(index)
                assertDimension(dimensionsOfSize(2), cell)
                assertArrayContent(arrayOf(v0, v1), cell)
            }
            assertDimension(dimensionsOfSize(3, 4), result)
            assertCell(0, 0, 1)
            assertCell(1, 1, 2)
            assertCell(2, 2, 3)
            assertCell(3, 3, 4)
            assertCell(4, 5, 6)
            assertCell(5, 6, 7)
            assertCell(6, 7, 8)
            assertCell(7, 8, 9)
            assertCell(8, 0, 1)
            assertCell(9, 1, 2)
            assertCell(10, 2, 3)
            assertCell(11, 3, 4)
        }
    }

    @Test
    fun twoDimensionalNWise() {
        parseAPLExpression("2+/3 4 ⍴ ⍳10").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertArrayContent(arrayOf(1, 3, 5, 9, 11, 13, 17, 9, 1), result)
        }
    }

    @Test
    fun smallArrayNWise() {
        parseAPLExpression("6+/1+⍳5").let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun failsWithNWiseOfTooSmallArray() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("5+/1 2 3")
        }
    }

    @Test
    fun twoDimensionalNWiseWithAxis() {
        parseAPLExpression("2,/[0] 3 5 ⍴ ⍳10").let { result ->
            fun assertCell(index: Int, v0: Int, v1: Int) {
                val cell = result.valueAt(index)
                assertDimension(dimensionsOfSize(2), cell)
                assertArrayContent(arrayOf(v0, v1), cell)
            }
            assertDimension(dimensionsOfSize(2, 5), result)
            assertCell(0, 0, 5)
            assertCell(1, 1, 6)
            assertCell(2, 2, 7)
            assertCell(3, 3, 8)
            assertCell(4, 4, 9)
            assertCell(5, 5, 0)
            assertCell(6, 6, 1)
            assertCell(7, 7, 2)
            assertCell(8, 8, 3)
            assertCell(9, 9, 4)
        }
    }

    @Test
    fun fourDimensionalNWiseWithAxis() {
        parseAPLExpression("2+/[1] 2 4 3 3 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(2, 3, 3, 3), result)
            assertArrayContent(
                arrayOf(
                    9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39, 41, 43,
                    45, 47, 49, 51, 53, 55, 57, 59, 61, 81, 83, 85, 87, 89, 91, 93, 95,
                    97, 99, 101, 103, 105, 107, 109, 111, 113, 115, 117, 119, 121, 123,
                    125, 127, 129, 131, 133
                ), result)
        }
    }

    @Test
    fun twoDimensionalNWiseWithNullResult() {
        parseAPLExpression("5 ,/ 3 4 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(3, 0), result)
        }
    }

    @Test
    fun nestedScalarReduce0() {
        parseAPLExpression("+/ (1 2) (3 4)").let { result ->
            assertDimension(emptyDimensions(), result)
            assert1DArray(arrayOf(4, 6), result.valueAt(0))
        }
    }

    @Test
    fun nestedScalarReduce1() {
        parseAPLExpression("+/ (1 2) (2 3) (3 4)").let { result ->
            assertDimension(emptyDimensions(), result)
            assert1DArray(arrayOf(6, 9), result.valueAt(0))
        }
    }

    @Test
    fun nestedScalarReduce2() {
        parseAPLExpression("+/ ((10 20) (30 40)) ((50 60) (70 80)) ((90 100) (110 120))").let { result ->
            assertDimension(emptyDimensions(), result)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), inner)
            assert1DArray(arrayOf(150, 180), inner.valueAt(0))
            assert1DArray(arrayOf(210, 240), inner.valueAt(1))
        }
    }

    @Test
    fun nestedScalarReduce3() {
        parseAPLExpression("+/ ((10 20) (30 40)) (100 200) ((50 60) (70 80)) ((90 100) (110 120))").let { result ->
            assertDimension(emptyDimensions(), result)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), inner)
            assert1DArray(arrayOf(250, 280), inner.valueAt(0))
            assert1DArray(arrayOf(410, 440), inner.valueAt(1))
        }
    }

    @Test
    fun nestedScalarReduce4() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("+[2]/ ((10 20) (30 40)) (100 200) ((50 60) (70 80)) ((90 100) (110 120))")
        }
    }

    @Test
    fun nestedScalarReduceWithAxis0() {
        parseAPLExpression("+[0]/ (1 2) (2 2 ⍴ 3 4 5 6)").let { result ->
            assertDimension(emptyDimensions(), result)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(2, 2), inner)
            assertArrayContent(arrayOf(4, 5, 7, 8), inner)
        }
    }

    @Test
    fun nestedScalarReduceWithAxis1() {
        parseAPLExpression("+[1]/ (1 2) (2 2 ⍴ 3 4 5 6)").let { result ->
            assertDimension(emptyDimensions(), result)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(2, 2), inner)
            assertArrayContent(arrayOf(4, 6, 6, 8), inner)
        }
    }
}
