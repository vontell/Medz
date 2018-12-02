package org.vontech.medz

import org.junit.Test

/**
 * Created by vontell on 12/1/18.
 */
class StringUtilsTests {

    @Test
    fun regexFindingShouldWork() {

        val test1 = "UIN 125 MCG with 120 capsules"
        val test2 = "UIN 125 iU with 120 capsules"
        val test3 = "UIN 125 MCG with 120 tablets"
        val test4 = "UIN 1 MCG with QTY: 90"
        val test5 = "UIN 125 MCG with 120 softgels"

        val textScanner = TextScan()
        textScanner.scannedTexts.add(test1)
        println(getMedicationResult(textScanner))

    }

    @Test
    fun findFrequency() {

        val test1 = "For adults, take one (1)\nsoftgel daily, preferably"
        val test2 = "TAKE 1 TABLET "
        val test3 = "UIN 125 MCG with 120 tablets"
        val test4 = "UIN 1 MCG with QTY: 90"
        val test5 = "UIN 125 MCG with 120 softgels"

        val textScanner = TextScan()
        textScanner.scannedTexts.add(test1)
        println(getMedicationResult(textScanner))

    }

}