package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TransposeTest : APLTest() {
    @Test
    fun test2DTransposeEmptyLeftArg() {
        val result = parseAPLExpression("⍉ 2 3 ⍴ ⍳6")
        assertDimension(dimensionsOfSize(3, 2), result)
        assertArrayContent(arrayOf(0, 3, 1, 4, 2, 5), result)
    }

    @Test
    fun test2DTranspose() {
        val result = parseAPLExpression("1 0 ⍉ 2 3 ⍴ ⍳6")
        assertDimension(dimensionsOfSize(3, 2), result)
        assertArrayContent(arrayOf(0, 3, 1, 4, 2, 5), result)
    }                //0, 4, 8, 12, 16, 20, 1, 5, 9, 13, 17, 21, 2, 6, 10, 14, 18, 22, 3, 7, 11, 15, 19, 23

    @Test
    fun test2DTransposeInverse() {
        val result = parseAPLExpression("⍉˝ 2 3 ⍴ ⍳6")
        assertDimension(dimensionsOfSize(3, 2), result)
        assertArrayContent(arrayOf(0, 3, 1, 4, 2, 5), result)
    }

    @Test
    fun test3DTransposeEmptyLeftArg() {
        val result = parseAPLExpression("⍉ 3 4 5 ⍴ ⍳60")
        assertDimension(dimensionsOfSize(5, 4, 3), result)
        assertArrayContent(
            arrayOf(
                0, 20, 40, 5, 25, 45, 10, 30, 50, 15, 35, 55, 1, 21, 41, 6, 26, 46,
                11, 31, 51, 16, 36, 56, 2, 22, 42, 7, 27, 47, 12, 32, 52, 17, 37, 57,
                3, 23, 43, 8, 28, 48, 13, 33, 53, 18, 38, 58, 4, 24, 44, 9, 29, 49,
                14, 34, 54, 19, 39, 59),
            result)
    }

    @Test
    fun test4DTranspose() {
        val result = parseAPLExpression("2 3 0 1 ⍉ 2 3 4 5 ⍴ ⍳120")
        assertDimension(dimensionsOfSize(4, 5, 2, 3), result)
        assertArrayContent(
            arrayOf(
                0, 20, 40, 60, 80, 100, 1, 21, 41, 61, 81, 101, 2, 22, 42, 62, 82,
                102, 3, 23, 43, 63, 83, 103, 4, 24, 44, 64, 84, 104, 5, 25, 45, 65,
                85, 105, 6, 26, 46, 66, 86, 106, 7, 27, 47, 67, 87, 107, 8, 28, 48,
                68, 88, 108, 9, 29, 49, 69, 89, 109, 10, 30, 50, 70, 90, 110, 11, 31,
                51, 71, 91, 111, 12, 32, 52, 72, 92, 112, 13, 33, 53, 73, 93, 113, 14,
                34, 54, 74, 94, 114, 15, 35, 55, 75, 95, 115, 16, 36, 56, 76, 96, 116,
                17, 37, 57, 77, 97, 117, 18, 38, 58, 78, 98, 118, 19, 39, 59, 79, 99,
                119),
            result)
    }

    @Test
    fun renderTransposedArray() {
        val result = parseAPLExpression("2 3 1 0 ⍉ 2 3 4 5 ⍴ ⍳120")
        assertTrue(result.formatted(FormatStyle.PRETTY).length > 100)
    }

    @Test
    fun test5DTranspose() {
        val result = parseAPLExpression("1 4 2 0 3 ⍉ 2 3 4 5 6 ⍴ ⍳720")
        assertDimension(dimensionsOfSize(5, 2, 4, 6, 3), result)
        assertArrayContent(
            arrayOf(
                0, 120, 240, 1, 121, 241, 2, 122, 242, 3, 123, 243, 4, 124, 244, 5,
                125, 245, 30, 150, 270, 31, 151, 271, 32, 152, 272, 33, 153, 273, 34,
                154, 274, 35, 155, 275, 60, 180, 300, 61, 181, 301, 62, 182, 302, 63,
                183, 303, 64, 184, 304, 65, 185, 305, 90, 210, 330, 91, 211, 331, 92,
                212, 332, 93, 213, 333, 94, 214, 334, 95, 215, 335, 360, 480, 600,
                361, 481, 601, 362, 482, 602, 363, 483, 603, 364, 484, 604, 365, 485,
                605, 390, 510, 630, 391, 511, 631, 392, 512, 632, 393, 513, 633, 394,
                514, 634, 395, 515, 635, 420, 540, 660, 421, 541, 661, 422, 542, 662,
                423, 543, 663, 424, 544, 664, 425, 545, 665, 450, 570, 690, 451, 571,
                691, 452, 572, 692, 453, 573, 693, 454, 574, 694, 455, 575, 695, 6,
                126, 246, 7, 127, 247, 8, 128, 248, 9, 129, 249, 10, 130, 250, 11,
                131, 251, 36, 156, 276, 37, 157, 277, 38, 158, 278, 39, 159, 279, 40,
                160, 280, 41, 161, 281, 66, 186, 306, 67, 187, 307, 68, 188, 308, 69,
                189, 309, 70, 190, 310, 71, 191, 311, 96, 216, 336, 97, 217, 337, 98,
                218, 338, 99, 219, 339, 100, 220, 340, 101, 221, 341, 366, 486, 606,
                367, 487, 607, 368, 488, 608, 369, 489, 609, 370, 490, 610, 371, 491,
                611, 396, 516, 636, 397, 517, 637, 398, 518, 638, 399, 519, 639, 400,
                520, 640, 401, 521, 641, 426, 546, 666, 427, 547, 667, 428, 548, 668,
                429, 549, 669, 430, 550, 670, 431, 551, 671, 456, 576, 696, 457, 577,
                697, 458, 578, 698, 459, 579, 699, 460, 580, 700, 461, 581, 701, 12,
                132, 252, 13, 133, 253, 14, 134, 254, 15, 135, 255, 16, 136, 256, 17,
                137, 257, 42, 162, 282, 43, 163, 283, 44, 164, 284, 45, 165, 285, 46,
                166, 286, 47, 167, 287, 72, 192, 312, 73, 193, 313, 74, 194, 314, 75,
                195, 315, 76, 196, 316, 77, 197, 317, 102, 222, 342, 103, 223, 343,
                104, 224, 344, 105, 225, 345, 106, 226, 346, 107, 227, 347, 372, 492,
                612, 373, 493, 613, 374, 494, 614, 375, 495, 615, 376, 496, 616, 377,
                497, 617, 402, 522, 642, 403, 523, 643, 404, 524, 644, 405, 525, 645,
                406, 526, 646, 407, 527, 647, 432, 552, 672, 433, 553, 673, 434, 554,
                674, 435, 555, 675, 436, 556, 676, 437, 557, 677, 462, 582, 702, 463,
                583, 703, 464, 584, 704, 465, 585, 705, 466, 586, 706, 467, 587, 707,
                18, 138, 258, 19, 139, 259, 20, 140, 260, 21, 141, 261, 22, 142, 262,
                23, 143, 263, 48, 168, 288, 49, 169, 289, 50, 170, 290, 51, 171, 291,
                52, 172, 292, 53, 173, 293, 78, 198, 318, 79, 199, 319, 80, 200, 320,
                81, 201, 321, 82, 202, 322, 83, 203, 323, 108, 228, 348, 109, 229,
                349, 110, 230, 350, 111, 231, 351, 112, 232, 352, 113, 233, 353, 378,
                498, 618, 379, 499, 619, 380, 500, 620, 381, 501, 621, 382, 502, 622,
                383, 503, 623, 408, 528, 648, 409, 529, 649, 410, 530, 650, 411, 531,
                651, 412, 532, 652, 413, 533, 653, 438, 558, 678, 439, 559, 679, 440,
                560, 680, 441, 561, 681, 442, 562, 682, 443, 563, 683, 468, 588, 708,
                469, 589, 709, 470, 590, 710, 471, 591, 711, 472, 592, 712, 473, 593,
                713, 24, 144, 264, 25, 145, 265, 26, 146, 266, 27, 147, 267, 28, 148,
                268, 29, 149, 269, 54, 174, 294, 55, 175, 295, 56, 176, 296, 57, 177,
                297, 58, 178, 298, 59, 179, 299, 84, 204, 324, 85, 205, 325, 86, 206,
                326, 87, 207, 327, 88, 208, 328, 89, 209, 329, 114, 234, 354, 115,
                235, 355, 116, 236, 356, 117, 237, 357, 118, 238, 358, 119, 239, 359,
                384, 504, 624, 385, 505, 625, 386, 506, 626, 387, 507, 627, 388, 508,
                628, 389, 509, 629, 414, 534, 654, 415, 535, 655, 416, 536, 656, 417,
                537, 657, 418, 538, 658, 419, 539, 659, 444, 564, 684, 445, 565, 685,
                446, 566, 686, 447, 567, 687, 448, 568, 688, 449, 569, 689, 474, 594,
                714, 475, 595, 715, 476, 596, 716, 477, 597, 717, 478, 598, 718, 479,
                599, 719),
            result)
    }

    @Test
    fun transposeSingleTestEmptyLeftArg() {
        val result = parseAPLExpression("⍉3")
        assertDimension(emptyDimensions(), result)
        assertEquals(3, result.ensureNumber().asLong())
    }

    @Test
    fun transposeSingleTest() {
        val result = parseAPLExpression("(0⍴7) ⍉3")
        assertDimension(emptyDimensions(), result)
        assertEquals(3, result.ensureNumber().asLong())
    }

    @Test
    fun errorWithIncorrectAxisCount() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 ⍉ 3 4 5 ⍴ ⍳60")
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 2 3 ⍉ 3 4 5 ⍴ ⍳60")
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 3 ⍉ 3 4 5 ⍴ ⍳60")
        }
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 3 ⍉ 3 4 5 ⍴ ⍳60")
        }
    }

    @Test
    fun reverseHorizontalTest() {
        parseAPLExpression("⌽4 5 4 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(4, 5, 4), result)
            assertArrayContent(
                arrayOf(
                    3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12, 19, 18, 17, 16, 23, 22,
                    21, 20, 27, 26, 25, 24, 31, 30, 29, 28, 35, 34, 33, 32, 39, 38, 37, 36, 43,
                    42, 41, 40, 47, 46, 45, 44, 51, 50, 49, 48, 55, 54, 53, 52, 59, 58, 57, 56, 63,
                    62, 61, 60, 67, 66, 65, 64, 71, 70, 69, 68, 75, 74, 73, 72, 79, 78, 77, 76), result)
        }
        parseAPLExpression("⌽1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(4, 3, 2, 1), result)
        }
        parseAPLExpression("⌽1").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun reverseHorizontalInverse() {
        parseAPLExpression("⌽˝ 1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(4, 3, 2, 1), result)
        }
    }

    @Test
    fun reverseVerticalTest() {
        parseAPLExpression("⊖4 5 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(4, 5), result)
            assertArrayContent(arrayOf(15, 16, 17, 18, 19, 10, 11, 12, 13, 14, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4), result)
        }
        parseAPLExpression("⊖1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(4, 3, 2, 1), result)
        }
        parseAPLExpression("⊖1").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun reverseVerticalInverse() {
        parseAPLExpression("⊖˝ 1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(4, 3, 2, 1), result)
        }
    }

    @Test
    fun reverseWithAxis() {
        parseAPLExpression("⌽[1] 2 3 4 ⍴ ⍳24").let { result ->
            assertDimension(dimensionsOfSize(2, 3, 4), result)
            assertArrayContent(
                arrayOf(
                    8, 9, 10, 11,
                    4, 5, 6, 7,
                    0, 1, 2, 3,
                    20, 21, 22, 23,
                    16, 17, 18, 19,
                    12, 13, 14, 15),
                result)
        }
    }

    @Test
    fun reverseWithAxisInverse() {
        parseAPLExpression("⌽[1]˝ 2 3 4 ⍴ ⍳24").let { result ->
            assertDimension(dimensionsOfSize(2, 3, 4), result)
            assertArrayContent(
                arrayOf(
                    8, 9, 10, 11,
                    4, 5, 6, 7,
                    0, 1, 2, 3,
                    20, 21, 22, 23,
                    16, 17, 18, 19,
                    12, 13, 14, 15),
                result)
        }
    }

    @Test
    fun rotateHorizontalTest() {
        parseAPLExpression("1⌽1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(2, 3, 4, 1), result)
        }
    }

    @Test
    fun rotateVerticalTest() {
        parseAPLExpression("1⊖4 5 ⍴ ⍳100").let { result ->
            assertDimension(dimensionsOfSize(4, 5), result)
            assertArrayContent(arrayOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 0, 1, 2, 3, 4), result)
        }
    }

    @Test
    fun rotateLeftWithArrayLeftArg() {
        parseAPLExpression("1 2 3 ⌽ 3 3 ⍴ ⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertArrayContent(arrayOf(1, 2, 0, 5, 3, 4, 6, 7, 8), result)
        }
    }

    @Test
    fun rotateLeftWithArrayArgIndex0() {
        parseAPLExpression("(4 2 2 3 2 ⍴ 1 1 2 3 1) ⌽[0] 5 4 2 2 3 2 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(5, 4, 2, 2, 3, 2), result)
            assertArrayContent(
                arrayOf(
                    96, 97, 194, 291, 100, 101, 102, 199, 296, 105, 106, 107, 204, 301,
                    110, 111, 112, 209, 306, 115, 116, 117, 214, 311, 120, 121, 122, 219,
                    316, 125, 126, 127, 224, 321, 130, 131, 132, 229, 326, 135, 136, 137,
                    234, 331, 140, 141, 142, 239, 336, 145, 146, 147, 244, 341, 150, 151,
                    152, 249, 346, 155, 156, 157, 254, 351, 160, 161, 162, 259, 356, 165,
                    166, 167, 264, 361, 170, 171, 172, 269, 366, 175, 176, 177, 274, 371,
                    180, 181, 182, 279, 376, 185, 186, 187, 284, 381, 190, 191, 192, 193,
                    290, 387, 196, 197, 198, 295, 392, 201, 202, 203, 300, 397, 206, 207,
                    208, 305, 402, 211, 212, 213, 310, 407, 216, 217, 218, 315, 412, 221,
                    222, 223, 320, 417, 226, 227, 228, 325, 422, 231, 232, 233, 330, 427,
                    236, 237, 238, 335, 432, 241, 242, 243, 340, 437, 246, 247, 248, 345,
                    442, 251, 252, 253, 350, 447, 256, 257, 258, 355, 452, 261, 262, 263,
                    360, 457, 266, 267, 268, 365, 462, 271, 272, 273, 370, 467, 276, 277,
                    278, 375, 472, 281, 282, 283, 380, 477, 286, 287, 288, 289, 386, 3,
                    292, 293, 294, 391, 8, 297, 298, 299, 396, 13, 302, 303, 304, 401, 18,
                    307, 308, 309, 406, 23, 312, 313, 314, 411, 28, 317, 318, 319, 416,
                    33, 322, 323, 324, 421, 38, 327, 328, 329, 426, 43, 332, 333, 334,
                    431, 48, 337, 338, 339, 436, 53, 342, 343, 344, 441, 58, 347, 348,
                    349, 446, 63, 352, 353, 354, 451, 68, 357, 358, 359, 456, 73, 362,
                    363, 364, 461, 78, 367, 368, 369, 466, 83, 372, 373, 374, 471, 88,
                    377, 378, 379, 476, 93, 382, 383, 384, 385, 2, 99, 388, 389, 390, 7,
                    104, 393, 394, 395, 12, 109, 398, 399, 400, 17, 114, 403, 404, 405,
                    22, 119, 408, 409, 410, 27, 124, 413, 414, 415, 32, 129, 418, 419,
                    420, 37, 134, 423, 424, 425, 42, 139, 428, 429, 430, 47, 144, 433,
                    434, 435, 52, 149, 438, 439, 440, 57, 154, 443, 444, 445, 62, 159,
                    448, 449, 450, 67, 164, 453, 454, 455, 72, 169, 458, 459, 460, 77,
                    174, 463, 464, 465, 82, 179, 468, 469, 470, 87, 184, 473, 474, 475,
                    92, 189, 478, 479, 0, 1, 98, 195, 4, 5, 6, 103, 200, 9, 10, 11, 108,
                    205, 14, 15, 16, 113, 210, 19, 20, 21, 118, 215, 24, 25, 26, 123, 220,
                    29, 30, 31, 128, 225, 34, 35, 36, 133, 230, 39, 40, 41, 138, 235, 44,
                    45, 46, 143, 240, 49, 50, 51, 148, 245, 54, 55, 56, 153, 250, 59, 60,
                    61, 158, 255, 64, 65, 66, 163, 260, 69, 70, 71, 168, 265, 74, 75, 76,
                    173, 270, 79, 80, 81, 178, 275, 84, 85, 86, 183, 280, 89, 90, 91, 188,
                    285, 94, 95
                ), result)
        }
    }

    @Test
    fun rotateLeftWithArrayArgIndex4() {
        parseAPLExpression("(5 4 2 2 2 ⍴ 1 1 2 3 1) ⌽[4] 5 4 2 2 3 2 ⍴ ⍳1000").let { result ->
            assertDimension(dimensionsOfSize(5, 4, 2, 2, 3, 2), result)
            assertArrayContent(
                arrayOf(
                    2, 3, 4, 5, 0, 1, 10, 7, 6, 9, 8, 11, 14, 15, 16, 17, 12, 13, 20, 23,
                    22, 19, 18, 21, 24, 27, 26, 29, 28, 25, 32, 33, 34, 35, 30, 31, 40,
                    37, 36, 39, 38, 41, 44, 45, 46, 47, 42, 43, 50, 53, 52, 49, 48, 51,
                    54, 57, 56, 59, 58, 55, 62, 63, 64, 65, 60, 61, 70, 67, 66, 69, 68,
                    71, 74, 75, 76, 77, 72, 73, 80, 83, 82, 79, 78, 81, 84, 87, 86, 89,
                    88, 85, 92, 93, 94, 95, 90, 91, 100, 97, 96, 99, 98, 101, 104, 105,
                    106, 107, 102, 103, 110, 113, 112, 109, 108, 111, 114, 117, 116, 119,
                    118, 115, 122, 123, 124, 125, 120, 121, 130, 127, 126, 129, 128, 131,
                    134, 135, 136, 137, 132, 133, 140, 143, 142, 139, 138, 141, 144, 147,
                    146, 149, 148, 145, 152, 153, 154, 155, 150, 151, 160, 157, 156, 159,
                    158, 161, 164, 165, 166, 167, 162, 163, 170, 173, 172, 169, 168, 171,
                    174, 177, 176, 179, 178, 175, 182, 183, 184, 185, 180, 181, 190, 187,
                    186, 189, 188, 191, 194, 195, 196, 197, 192, 193, 200, 203, 202, 199,
                    198, 201, 204, 207, 206, 209, 208, 205, 212, 213, 214, 215, 210, 211,
                    220, 217, 216, 219, 218, 221, 224, 225, 226, 227, 222, 223, 230, 233,
                    232, 229, 228, 231, 234, 237, 236, 239, 238, 235, 242, 243, 244, 245,
                    240, 241, 250, 247, 246, 249, 248, 251, 254, 255, 256, 257, 252, 253,
                    260, 263, 262, 259, 258, 261, 264, 267, 266, 269, 268, 265, 272, 273,
                    274, 275, 270, 271, 280, 277, 276, 279, 278, 281, 284, 285, 286, 287,
                    282, 283, 290, 293, 292, 289, 288, 291, 294, 297, 296, 299, 298, 295,
                    302, 303, 304, 305, 300, 301, 310, 307, 306, 309, 308, 311, 314, 315,
                    316, 317, 312, 313, 320, 323, 322, 319, 318, 321, 324, 327, 326, 329,
                    328, 325, 332, 333, 334, 335, 330, 331, 340, 337, 336, 339, 338, 341,
                    344, 345, 346, 347, 342, 343, 350, 353, 352, 349, 348, 351, 354, 357,
                    356, 359, 358, 355, 362, 363, 364, 365, 360, 361, 370, 367, 366, 369,
                    368, 371, 374, 375, 376, 377, 372, 373, 380, 383, 382, 379, 378, 381,
                    384, 387, 386, 389, 388, 385, 392, 393, 394, 395, 390, 391, 400, 397,
                    396, 399, 398, 401, 404, 405, 406, 407, 402, 403, 410, 413, 412, 409,
                    408, 411, 414, 417, 416, 419, 418, 415, 422, 423, 424, 425, 420, 421,
                    430, 427, 426, 429, 428, 431, 434, 435, 436, 437, 432, 433, 440, 443,
                    442, 439, 438, 441, 444, 447, 446, 449, 448, 445, 452, 453, 454, 455,
                    450, 451, 460, 457, 456, 459, 458, 461, 464, 465, 466, 467, 462, 463,
                    470, 473, 472, 469, 468, 471, 474, 477, 476, 479, 478, 475
                ), result)
        }
    }

    @Test
    fun reverseHorizontalInvalidAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⌽[¯1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⌽[1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⌽[2] 4 5 ⍴ ⍳4")
        }
    }

    @Test
    fun rotateHorizontalInvalidAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("2 ⌽[¯1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("2 ⌽[1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("2 ⌽[2] 4 5 ⍴ ⍳4")
        }
    }

    @Test
    fun reverseVerticallInvalidAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⊖[¯1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⊖[1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("⊖[2] 4 5 ⍴ ⍳4")
        }
    }

    @Test
    fun rotateVerticallInvalidAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("2 ⊖[¯1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("2 ⊖[1] 1 2 3")
        }
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("2 ⊖[2] 4 5 ⍴ ⍳4")
        }
    }
}
