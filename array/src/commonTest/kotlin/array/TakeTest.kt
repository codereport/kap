package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TakeTest : APLTest() {
    @Test
    fun testDropSimple() {
        val result = parseAPLExpression("↓1 2 3 4")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(2, 3, 4), result)
    }

    @Test
    fun testDropFunctionResult() {
        val result = parseAPLExpression("↓10 + 1 2 3 4")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(12, 13, 14), result)
    }

    @Test
    fun testDropFromArray1() {
        val result = parseAPLExpression("↓,1")
        assertDimension(dimensionsOfSize(0), result)
        assertEquals(0, result.size)
    }

    @Test
    fun testTakeSimple() {
        val result = parseAPLExpression("↑1 2 3 4")
        assertTrue(result.isScalar())
        assertEquals(1L, result.ensureNumber().asLong())
    }

    @Test
    fun testTakeFromArray1() {
        val result = parseAPLExpression("↑,1")
        assertTrue(result.isScalar())
        assertEquals(1L, result.ensureNumber().asLong())
    }

    @Test
    fun takeScalar0() {
        parseAPLExpression("↑6").let { result ->
            assertSimpleNumber(6, result)
        }
    }

    @Test
    fun testScalar1() {
        parseAPLExpression("↑⊂6 5").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(6, 5), result)
        }
    }

    @Test
    fun takeSingleDimension() {
        parseAPLExpression("3 ↑ 10 11 12 13 14 15 16 17").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 11, 12), result)
        }
    }

    @Test
    fun takeMultiDimension() {
        parseAPLExpression("2 3 ↑ 10 15 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(0, 1, 2, 15, 16, 17), result)
        }
    }

    @Test
    fun dropSingleDimension() {
        parseAPLExpression("2 ↓ 100 200 300 400 500 600 700 800").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(300, 400, 500, 600, 700, 800), result)
        }
    }

    @Test
    fun dropMultiDimension() {
        parseAPLExpression("20 34 ↓ 30 40 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(10, 6), result)
            assertArrayContent(
                arrayOf(
                    34, 35, 36, 37, 38, 39, 74, 75, 76, 77, 78, 79, 14, 15, 16, 17, 18,
                    19, 54, 55, 56, 57, 58, 59, 94, 95, 96, 97, 98, 99, 34, 35, 36, 37,
                    38, 39, 74, 75, 76, 77, 78, 79, 14, 15, 16, 17, 18, 19, 54, 55, 56,
                    57, 58, 59, 94, 95, 96, 97, 98, 99
                ), result)
        }
    }

    @Test
    fun fourDimensionalDrop() {
        parseAPLExpression("10 ¯20 ¯32 2 ↓ 14 30 40 4 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(4, 10, 8, 2), result)
            assertArrayContent(
                arrayOf(
                    2, 3, 6, 7, 10, 11, 14, 15, 18, 19, 22, 23, 26, 27, 30, 31, 162, 163,
                    166, 167, 170, 171, 174, 175, 178, 179, 182, 183, 186, 187, 190, 191,
                    322, 323, 326, 327, 330, 331, 334, 335, 338, 339, 342, 343, 346, 347,
                    350, 351, 482, 483, 486, 487, 490, 491, 494, 495, 498, 499, 502, 503,
                    506, 507, 510, 511, 642, 643, 646, 647, 650, 651, 654, 655, 658, 659,
                    662, 663, 666, 667, 670, 671, 802, 803, 806, 807, 810, 811, 814, 815,
                    818, 819, 822, 823, 826, 827, 830, 831, 962, 963, 966, 967, 970, 971,
                    974, 975, 978, 979, 982, 983, 986, 987, 990, 991, 122, 123, 126, 127,
                    130, 131, 134, 135, 138, 139, 142, 143, 146, 147, 150, 151, 282, 283,
                    286, 287, 290, 291, 294, 295, 298, 299, 302, 303, 306, 307, 310, 311,
                    442, 443, 446, 447, 450, 451, 454, 455, 458, 459, 462, 463, 466, 467,
                    470, 471, 802, 803, 806, 807, 810, 811, 814, 815, 818, 819, 822, 823,
                    826, 827, 830, 831, 962, 963, 966, 967, 970, 971, 974, 975, 978, 979,
                    982, 983, 986, 987, 990, 991, 122, 123, 126, 127, 130, 131, 134, 135,
                    138, 139, 142, 143, 146, 147, 150, 151, 282, 283, 286, 287, 290, 291,
                    294, 295, 298, 299, 302, 303, 306, 307, 310, 311, 442, 443, 446, 447,
                    450, 451, 454, 455, 458, 459, 462, 463, 466, 467, 470, 471, 602, 603,
                    606, 607, 610, 611, 614, 615, 618, 619, 622, 623, 626, 627, 630, 631,
                    762, 763, 766, 767, 770, 771, 774, 775, 778, 779, 782, 783, 786, 787,
                    790, 791, 922, 923, 926, 927, 930, 931, 934, 935, 938, 939, 942, 943,
                    946, 947, 950, 951, 82, 83, 86, 87, 90, 91, 94, 95, 98, 99, 102, 103,
                    106, 107, 110, 111, 242, 243, 246, 247, 250, 251, 254, 255, 258, 259,
                    262, 263, 266, 267, 270, 271, 602, 603, 606, 607, 610, 611, 614, 615,
                    618, 619, 622, 623, 626, 627, 630, 631, 762, 763, 766, 767, 770, 771,
                    774, 775, 778, 779, 782, 783, 786, 787, 790, 791, 922, 923, 926, 927,
                    930, 931, 934, 935, 938, 939, 942, 943, 946, 947, 950, 951, 82, 83,
                    86, 87, 90, 91, 94, 95, 98, 99, 102, 103, 106, 107, 110, 111, 242,
                    243, 246, 247, 250, 251, 254, 255, 258, 259, 262, 263, 266, 267, 270,
                    271, 402, 403, 406, 407, 410, 411, 414, 415, 418, 419, 422, 423, 426,
                    427, 430, 431, 562, 563, 566, 567, 570, 571, 574, 575, 578, 579, 582,
                    583, 586, 587, 590, 591, 722, 723, 726, 727, 730, 731, 734, 735, 738,
                    739, 742, 743, 746, 747, 750, 751, 882, 883, 886, 887, 890, 891, 894,
                    895, 898, 899, 902, 903, 906, 907, 910, 911, 42, 43, 46, 47, 50, 51,
                    54, 55, 58, 59, 62, 63, 66, 67, 70, 71, 402, 403, 406, 407, 410, 411,
                    414, 415, 418, 419, 422, 423, 426, 427, 430, 431, 562, 563, 566, 567,
                    570, 571, 574, 575, 578, 579, 582, 583, 586, 587, 590, 591, 722, 723,
                    726, 727, 730, 731, 734, 735, 738, 739, 742, 743, 746, 747, 750, 751,
                    882, 883, 886, 887, 890, 891, 894, 895, 898, 899, 902, 903, 906, 907,
                    910, 911, 42, 43, 46, 47, 50, 51, 54, 55, 58, 59, 62, 63, 66, 67, 70,
                    71, 202, 203, 206, 207, 210, 211, 214, 215, 218, 219, 222, 223, 226,
                    227, 230, 231, 362, 363, 366, 367, 370, 371, 374, 375, 378, 379, 382,
                    383, 386, 387, 390, 391, 522, 523, 526, 527, 530, 531, 534, 535, 538,
                    539, 542, 543, 546, 547, 550, 551, 682, 683, 686, 687, 690, 691, 694,
                    695, 698, 699, 702, 703, 706, 707, 710, 711, 842, 843, 846, 847, 850,
                    851, 854, 855, 858, 859, 862, 863, 866, 867, 870, 871
                ), result)
        }
    }

    @Test
    fun takeWithNegativeArg() {
        parseAPLExpression("¯2 ↑ 100 200 300 400 500 600 700 800 900 1000 1100, 1200").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1100, 1200), result)
        }
    }

    @Test
    fun takeMultiDimensionalWithNegativeArg() {
        parseAPLExpression("¯17 ¯20 ↑ 30 40 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(17, 20), result)
            assertArrayContent(
                arrayOf(
                    540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553,
                    554, 555, 556, 557, 558, 559, 580, 581, 582, 583, 584, 585, 586, 587,
                    588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 620, 621,
                    622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635,
                    636, 637, 638, 639, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669,
                    670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 700, 701, 702, 703,
                    704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717,
                    718, 719, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751,
                    752, 753, 754, 755, 756, 757, 758, 759, 780, 781, 782, 783, 784, 785,
                    786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799,
                    820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833,
                    834, 835, 836, 837, 838, 839, 860, 861, 862, 863, 864, 865, 866, 867,
                    868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879, 900, 901,
                    902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 912, 913, 914, 915,
                    916, 917, 918, 919, 940, 941, 942, 943, 944, 945, 946, 947, 948, 949,
                    950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 980, 981, 982, 983,
                    984, 985, 986, 987, 988, 989, 990, 991, 992, 993, 994, 995, 996, 997,
                    998, 999, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
                    35, 36, 37, 38, 39, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
                    72, 73, 74, 75, 76, 77, 78, 79, 100, 101, 102, 103, 104, 105, 106,
                    107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 140,
                    141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154,
                    155, 156, 157, 158, 159, 180, 181, 182, 183, 184, 185, 186, 187, 188,
                    189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199
                ), result)
        }
    }

    @Test
    fun dropWithNegativeArg() {
        parseAPLExpression("¯2 ↓ 1 2 3 4 5 6").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 3, 4), result)
        }
    }

    @Test
    fun dropMultiDimensionalWithNegativeArg() {
        parseAPLExpression("¯26 ¯32 ↓ 30 40 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(4, 8), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 40, 41, 42, 43, 44, 45, 46, 47, 80, 81, 82,
                    83, 84, 85, 86, 87, 120, 121, 122, 123, 124, 125, 126, 127
                ), result)
        }
    }

    @Test
    fun takeFromEmpty() {
        assertSimpleNumber(0, parseAPLExpression("↑⍬"))
    }

    @Test
    fun takeExceedingInputDimension() {
        parseAPLExpression("4 ↑ 1 2").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 2, 0, 0), result)
        }
    }

    @Test
    fun takeExceedingInputDimensionNegativeLeftArg() {
        parseAPLExpression("¯10 ↑ 1 2").let { result ->
            assertDimension(dimensionsOfSize(10), result)
            assertArrayContent(arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 2), result)
        }
    }

    @Test
    fun textExceedingnputDimensionMultiDimensionInput() {
        parseAPLExpression("5 6 ↑ 3 4 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(5, 6), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 0, 0, 4, 5, 6, 7, 0, 0, 8, 9, 10, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), result)
        }
    }

    @Test
    fun testExceedingInputMultiDimensionNegativeLeftArg() {
        parseAPLExpression("5 6 ¯10 3 1 ↑ 3 4 5 6 7 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(5, 6, 10, 3, 1), result)
            assertArrayContent(
                arrayOf(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 14, 42, 49, 56, 84,
                    91, 98, 26, 33, 40, 68, 75, 82, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 10, 17, 24, 52, 59, 66, 94, 1, 8, 36, 43, 50, 78, 85, 92, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 27, 34, 62, 69, 76, 4, 11,
                    18, 46, 53, 60, 88, 95, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 30, 37, 44, 72, 79, 86, 14, 21, 28, 56, 63, 70, 98, 5, 12, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 40, 47, 54, 82, 89, 96, 24, 31, 38, 66, 73, 80, 8, 15, 22, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 57, 64, 92, 99, 6, 34,
                    41, 48, 76, 83, 90, 18, 25, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 60, 67, 74, 2, 9, 16, 44, 51, 58, 86, 93, 0, 28, 35, 42, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 70, 77, 84, 12, 19, 26, 54, 61,
                    68, 96, 3, 10, 38, 45, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 80, 87, 94, 22, 29, 36,
                    64, 71, 78, 6, 13, 20, 48, 55, 62, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 90, 97, 4, 32, 39, 46, 74, 81, 88, 16, 23, 30, 58, 65, 72, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 14, 42, 49, 56, 84,
                    91, 98, 26, 33, 40, 68, 75, 82, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 10, 17, 24, 52, 59, 66, 94, 1, 8, 36, 43, 50, 78, 85, 92, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0), result)
        }
    }

    @Test
    fun dropSkippedDimensions0() {
        parseAPLExpression("1 ↓ 3 5 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(2, 5), result)
            assertArrayContent(arrayOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14), result)
        }
    }

    @Test
    fun dropSkippedDimensions1() {
        parseAPLExpression("2 ↓ 5 5 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(3, 5), result)
            assertArrayContent(arrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24), result)
        }
    }

    @Test
    fun dropSkippedDimensions2() {
        parseAPLExpression("4 4 ↓ 5 5 5 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(1, 1, 5), result)
            assertArrayContent(arrayOf(20, 21, 22, 23, 24), result)
        }
    }

    @Test
    fun dropWithTooManyDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("1 2 ↓ 1 2 3 4")
        }
    }

    @Test
    fun dropLeftArgTooBigRank() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(1 1 ⍴ 1) ↓ 1 2 3 4 5 6")
        }
    }
}
