package org.liamjd.cantilever.lambda.handlebars

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.util.*

/**
 * A modification of the standard handlebars '#each iterator.
 * Use #take "5" to show only the first 5 items in the collection
 */
class TakeHelper : Helper<Any> {
    override fun apply(context: Any, options: Options?): Any {
        if (context is Iterable<*>) {
            val limit: Int
            if (options == null) {
                Int.MAX_VALUE
            } else {
                limit = if (options.params.isNotEmpty()) {
                    (options.param<String?>(0)?.toInt() ?: Int.MAX_VALUE)
                } else {
                    Int.MAX_VALUE
                }
                val buffer = options.buffer()
                val loop = context.iterator()
                val base: Int = options.hash("base", 0) ?: 0
                var index = base
                var even = index % 2 == 0
                val parent = options.context
                val fn = options.fn
                var limitCounter = 0
                while (loop.hasNext() && limitCounter < limit) {
                    val it = loop.next()
                    val itCtx = Context.newContext(parent, it)
                    itCtx.combine("@key", index)
                        .combine("@index", index)
                        .combine("@first", if (index == base) "first" else "")
                        .combine("@last", if (!loop.hasNext()) "last" else "")
                        .combine("@odd", if (even) "" else "odd")
                        .combine("@even", if (even) "even" else "")
                        // 1-based index
                        .combine("@index_1", index + 1)
                    buffer?.append(options.apply(fn, itCtx, Arrays.asList<Any>(it, index)))
                    index += 1
                    even = !even
                    limitCounter++
                }
                // empty?
                if (base == index) {
                    buffer?.append(options.inverse())
                }
                return buffer as Any
            }
        } else if (context != null) {
            var index = 0
            val loop = options?.propertySet(context)?.iterator()
            val parent = options?.context
            var first = true
            val buffer = options?.buffer()
            val fn = options?.fn
            while (loop!!.hasNext()) {
                val entry = loop.next() as Map.Entry<*, *>
                val key = entry.key
                val value = entry.value
                val itCtx = Context.newBuilder(parent, value)
                    .combine("@key", key)
                    .combine("@index", index)
                    .combine("@first", if (first) "first" else "")
                    .combine("@last", if (loop.hasNext()) "last" else "")
                    .build()
                buffer?.append(options.apply(fn, itCtx, Arrays.asList<Any>(value, key)))
                first = false
                index++
            }
            // empty?
            if (first) {
                buffer?.append(options.inverse())
            }
            return buffer as Any
        } else {
            return options?.inverse() as Any
        }
        return "" // end case, should never happen!
    }
}