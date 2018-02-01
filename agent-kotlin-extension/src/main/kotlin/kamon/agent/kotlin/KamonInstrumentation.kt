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

package kamon.agent.kotlin

import kamon.agent.api.instrumentation.InstrumentationDescription
import kamon.agent.libs.io.vavr.Function1
import kamon.agent.libs.net.bytebuddy.description.method.MethodDescription
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatcher
import kamon.agent.api.instrumentation.KamonInstrumentation as JKamonInstrumentation
import kamon.agent.libs.net.bytebuddy.matcher.ElementMatchers as BBMatchers

typealias Element = ElementMatcher.Junction<MethodDescription>

class KamonInstrumentation : JKamonInstrumentation() {

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

fun kamonInstrumentation(init: KamonInstrumentation.() -> Unit): KamonInstrumentation = KamonInstrumentation().apply(init)

fun <A, B> ((A) -> B).toVavrFunc(): Function1<A, B> = object : Function1<A, B> {
    override fun apply(v: A): B {
        return this@toVavrFunc(v)
    }
}

inline fun <reified T> InstrumentationDescription.Builder.withMixin(): InstrumentationDescription.Builder = withMixin({ T::class.java })
inline fun <reified T> InstrumentationDescription.Builder.withAdvisorFor(methodDescription: ElementMatcher.Junction<MethodDescription>): InstrumentationDescription.Builder =
        withAdvisorFor(methodDescription, { T::class.java })
