/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kanela.agent.kotlin

import kanela.agent.api.instrumentation.InstrumentationDescription
import kanela.agent.libs.io.vavr.Function1
import kanela.agent.libs.net.bytebuddy.description.method.MethodDescription
import kanela.agent.libs.net.bytebuddy.matcher.ElementMatcher
import kanela.agent.api.instrumentation.KanelaInstrumentation as JKanelaInstrumentation
import kanela.agent.libs.net.bytebuddy.matcher.ElementMatchers as BBMatchers

typealias Element = ElementMatcher.Junction<MethodDescription>

open class KanelaInstrumentation : JKanelaInstrumentation() {

    fun forSubtypeOf(name: String, instrumentationFun: InstrumentationDescription.Builder.() -> InstrumentationDescription.Builder) =
            super.forSubtypeOf({ name }, instrumentationFun.build().toVavrFunc())


    fun forSubtypeOf(names: List<String>, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription.Builder) =
            names.forEach { forSubtypeOf(it, instrumentationFun) }


    fun forTargetType(name: String, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) =
            super.forTargetType({ name }, instrumentationFun.toVavrFunc())


    fun forTargetType(names: List<String>, instrumentationFun: (InstrumentationDescription.Builder) -> InstrumentationDescription) =
            names.forEach { forTargetType(it, instrumentationFun) }


    fun isConstructor(): Element = BBMatchers.isConstructor()

    fun isAbstract(): Element = BBMatchers.isAbstract()

    fun method(name: String): Element = BBMatchers.named(name)

    fun takesArguments(quantity: Int): Element = BBMatchers.takesArguments(quantity)

    fun takesVarArguments(vararg classes: Class<*>): Element = BBMatchers.takesArguments(*classes)

    inline fun <reified T> takes1Argument(): Element = BBMatchers.takesArguments(T::class.java)

    inline fun <reified T, reified R> takes2Arguments(): Element = BBMatchers.takesArguments(T::class.java, R::class.java)

    inline fun <reified T, reified R, reified S> takes3Arguments(): Element = BBMatchers.takesArguments(T::class.java, R::class.java, S::class.java)

    inline fun <reified T> withArgument(index: Int): Element = BBMatchers.takesArgument(index, T::class.java)

    fun anyMethod(vararg names: String): Element = names.map { method(it) }.reduce { a, b -> a.or(b) }

    infix fun String.or(right: String): List<String> = listOf(this, right)

    infix fun List<String>.or(right: String): List<String> = this.plus(right)

    infix fun Element.and(right: Element): Element = this.and(right)

    private fun ((InstrumentationDescription.Builder) -> InstrumentationDescription.Builder).build(): (InstrumentationDescription.Builder) -> InstrumentationDescription = { builder ->
        invoke(builder).build()
    }

}

fun kamonInstrumentation(init: KanelaInstrumentation.() -> Unit): KanelaInstrumentation = KanelaInstrumentation().apply(init)

fun <A, B> ((A) -> B).toVavrFunc(): Function1<A, B> = object : Function1<A, B> {
    override fun apply(v: A): B {
        return this@toVavrFunc(v)
    }
}

inline fun <reified T> InstrumentationDescription.Builder.withMixin(): InstrumentationDescription.Builder = withMixin({ T::class.java })
inline fun <reified T> InstrumentationDescription.Builder.withAdvisorFor(methodDescription: ElementMatcher.Junction<MethodDescription>): InstrumentationDescription.Builder =
        withAdvisorFor(methodDescription, { T::class.java })
