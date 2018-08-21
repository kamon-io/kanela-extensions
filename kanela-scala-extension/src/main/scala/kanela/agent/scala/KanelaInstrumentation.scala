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

package kanela.agent.scala

import java.util.function.{BiFunction => JBifunction, Supplier => JSupplier}

import kanela.agent.api.instrumentation.{InstrumentationDescription, KanelaInstrumentation => JKanelalInstrumentation}
import kanela.agent.libs.io.vavr.{Function1 => JFunction1, Function2 => JFunction2, Function4 => JFunction4}
import kanela.agent.libs.net.bytebuddy.description.`type`.TypeDescription
import kanela.agent.libs.net.bytebuddy.description.method.MethodDescription
import kanela.agent.libs.net.bytebuddy.dynamic.DynamicType.Builder
import kanela.agent.libs.net.bytebuddy.implementation.MethodDelegation
import kanela.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kanela.agent.libs.net.bytebuddy.matcher.{ElementMatcher, ElementMatchers => BBMatchers}
import kanela.agent.libs.net.bytebuddy.utility.JavaModule

import scala.collection.immutable.Seq

trait KanelaInstrumentation extends JKanelalInstrumentation with MethodDescriptionSugar {

  private implicit def toJavaFunction2[A, B, C](f: (A, B) ⇒ C): JFunction2[A, B, C] =
    new JFunction2[A, B, C]() {
      def apply(t1: A, t2: B): C = f(t1, t2)
    }

  private implicit def toJavaFunction4[A, B, C, D, E](f: (A, B, C, D) ⇒ E): JFunction4[A, B, C, D, E] =
    new JFunction4[A, B, C, D, E]() {
      def apply(t1: A, t2: B, t3: C, t4: D): E = f(t1, t2, t3, t4)
    }

  private implicit def toJavaBiFunction[A, B, C](f: (A, B) ⇒ C): JBifunction[A, B, C] =
    new JBifunction[A, B, C]() {
      def apply(t: A, u: B): C = f(t, u)
    }

  implicit def toJavaSupplier[A](f: ⇒ A): JSupplier[A] = new JSupplier[A]() {
    def get: A = f
  }

  implicit def toJavaFunction1[A, B](f: (A) ⇒ B): JFunction1[A, B] =
    new JFunction1[A, B]() {
      def apply(t1: A): B = f(t1)
    }

  def forSubtypeOf(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    super.forSubtypeOf(name, builder)
  }

  def forSubtypeOf(names: Seq[String])(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    forRawMatching(BBMatchers.hasSuperType(anyTypes(names: _*)))(builder)
  }

  def forTargetType(name: String)(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    super.forTargetType(name, builder)
  }

  def forTargetType(names: Seq[String])(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    forRawMatching(anyTypes(names: _*))(builder)
  }

  def forRawMatching(matcher: ⇒ ElementMatcher[_ >: TypeDescription])(builder: InstrumentationDescription.Builder ⇒ InstrumentationDescription): Unit = {
    forRawMatching(matcher, builder)
  }

  implicit class OrSyntax(left: String) {
    def or(right: String): Seq[String] = Seq(left, right)
  }

  implicit class MultipleOrSyntax(names: Seq[String]) {
    def or(name: String): Seq[String] = names ++ Seq(name)
  }

  implicit class PimpInstrumentationBuilder(instrumentationBuilder: InstrumentationDescription.Builder) {
    def addTransformation(f: ⇒ (Builder[_], TypeDescription, ClassLoader, JavaModule) ⇒ Builder[_]) =
      instrumentationBuilder.withTransformation(f)

    def withInterceptorFor(method: Junction[MethodDescription], delegate: Class[_]) =
      withTransformationFor(method, delegate)

    def withInterceptorFor(method: Junction[MethodDescription], delegate: AnyRef) =
      withTransformationFor(method, delegate)

    private def withTransformationFor(method: Junction[MethodDescription], delegate: Class[_]) =
      addTransformation((builder, _, _, _) ⇒ builder.method(method).intercept(MethodDelegation.to(delegate)))

    private def withTransformationFor(method: Junction[MethodDescription], delegate: AnyRef) =
      addTransformation((builder, _, _, _) ⇒ builder.method(method).intercept(MethodDelegation.to(delegate)))
  }
}

trait MethodDescriptionSugar { self: JKanelalInstrumentation ⇒

  val Constructor = isConstructor()

  override def isConstructor(): Junction[MethodDescription] =
    BBMatchers.isConstructor()

  override def isAbstract(): Junction[MethodDescription] =
    BBMatchers.isAbstract()

  override def method(name: String): Junction[MethodDescription] =
    BBMatchers.named(name)

  def takesArguments(quantity: Int): Junction[MethodDescription] =
    BBMatchers.takesArguments(quantity)

  def takesArguments(classes: Class[_]*): Junction[MethodDescription] =
    BBMatchers.takesArguments(classes: _*)

  def withArgument(index: Int, `type`: Class[_]): Junction[MethodDescription] =
    BBMatchers.takesArgument(index, `type`)

  override def withArgument(`type`: Class[_]): Junction[MethodDescription] =
    withArgument(0, `type`)

  def anyMethod(names: String*): Junction[MethodDescription] =
    names.map(method).reduce((a, b) => a.or[MethodDescription](b): Junction[MethodDescription])
}
