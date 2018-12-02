package org.vontech.medz

/**
 * Created by vontell on 12/1/18.
 */

fun findInString(regexString: String, source: String, ignoreCase: Boolean = true): MatchResult? {

    val regex = if (ignoreCase) {
        Regex(regexString, RegexOption.IGNORE_CASE)
    } else {
        Regex(regexString)
    }
    return regex.find(source)

}