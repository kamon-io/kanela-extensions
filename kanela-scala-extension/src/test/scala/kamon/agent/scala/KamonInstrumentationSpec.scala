package kamon.agent.scala

import java.lang.instrument.Instrumentation

import kanela.agent.libs.net.bytebuddy.description.method.MethodDescription
import kanela.agent.libs.net.bytebuddy.matcher.ElementMatcher.Junction
import kanela.agent.util.conf.KanelaConfiguration
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any

class KamonInstrumentationSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  "a KanelaInstrumentation from kanela-scala-extension" when {
    "instrumenting with a single mixin" should {

      val instrumentationMock = mock(classOf[Instrumentation])
      val moduleConfigurationMock = mock(classOf[KanelaConfiguration.ModuleConfiguration])
      when(moduleConfigurationMock.shouldInjectInBootstrap()).thenReturn(false)

      val ki = new KanelaInstrumentation {
        forSubtypeOf("laala") { builder ⇒
          builder
            .withMixin(classOf[ExampleMixin])
            .build()
        }
      }

      "return a single transformation" in {
        val transformations = ki.collectTransformations(moduleConfigurationMock, instrumentationMock)
        transformations.size shouldBe 1
        val transformation = transformations.get(0)
        transformation.getMixins.size() shouldBe 1
        transformation.getBridges.size() shouldBe 0
        transformation.getTransformations.size() shouldBe 0
        transformation.isActive shouldBe true
        transformation.getElementMatcher.isDefined shouldBe true

        verify(moduleConfigurationMock).shouldInjectInBootstrap()
        verifyNoMoreInteractions(moduleConfigurationMock)
        verifyNoMoreInteractions(instrumentationMock)

      }
    }

    "instrumenting with a single mixin and for bootstrap injection" should {
      val instrumentationMock = mock(classOf[Instrumentation])
      val moduleConfigurationMock = mock(classOf[KanelaConfiguration.ModuleConfiguration])
      when(moduleConfigurationMock.shouldInjectInBootstrap()).thenReturn(true)

      val ki = new KanelaInstrumentation {
        forSubtypeOf("laala") { builder ⇒
          builder
            .withMixin(classOf[ExampleMixin])
            .build()
        }
      }

      "return a single transformation" in {
        val transformations = ki.collectTransformations(moduleConfigurationMock, instrumentationMock)
        transformations.size shouldBe 1
        val transformation = transformations.get(0)
        transformation.getMixins.size() shouldBe 1
        transformation.getBridges.size() shouldBe 0
        transformation.getTransformations.size() shouldBe 0
        transformation.isActive shouldBe true
        transformation.getElementMatcher.isDefined shouldBe true

        verify(moduleConfigurationMock).shouldInjectInBootstrap()
        verify(moduleConfigurationMock).getTempDir
        verify(instrumentationMock).appendToBootstrapClassLoaderSearch(any())
        verifyNoMoreInteractions(moduleConfigurationMock)
        verifyNoMoreInteractions(instrumentationMock)

      }
    }

    "instrumenting with mixin and advisor without bootstrap injection" should {
      val instrumentationMock = mock(classOf[Instrumentation])
      val moduleConfigurationMock = mock(classOf[KanelaConfiguration.ModuleConfiguration])
      when(moduleConfigurationMock.shouldInjectInBootstrap()).thenReturn(false)

      val ki = new KanelaInstrumentation {

        val methodMatcher: Junction[MethodDescription] = method("executeMethod")
          .and(takesArguments(classOf[String], classOf[Int]))

        forSubtypeOf("laala") { builder ⇒
          builder
            .withMixin(classOf[ExampleMixin])
            .withAdvisorFor(methodMatcher, classOf[ExampleAdvisor])
            .build()
        }
      }

      "return two transformations" in {
        val transformations = ki.collectTransformations(moduleConfigurationMock, instrumentationMock)
        transformations.size shouldBe 1
        val transformation = transformations.get(0)
        transformation.getMixins.size() shouldBe 1
        transformation.getBridges.size() shouldBe 0
        transformation.getTransformations.size() shouldBe 1
        transformation.isActive shouldBe true
        transformation.getElementMatcher.isDefined shouldBe true

        verify(moduleConfigurationMock).shouldInjectInBootstrap()
        verifyNoMoreInteractions(moduleConfigurationMock)
        verifyNoMoreInteractions(instrumentationMock)

      }
    }

    "instrumenting with mixin and advisor for bootstrap injection" should {
      val instrumentationMock = mock(classOf[Instrumentation])
      val moduleConfigurationMock = mock(classOf[KanelaConfiguration.ModuleConfiguration])
      when(moduleConfigurationMock.shouldInjectInBootstrap()).thenReturn(true)

      val ki = new KanelaInstrumentation {

        val methodMatcher: Junction[MethodDescription] = method("executeMethod")
          .and(takesArguments(classOf[String], classOf[Int]))

        forSubtypeOf("laala") { builder ⇒
          builder
            .withMixin(classOf[ExampleMixin])
            .withAdvisorFor(methodMatcher, classOf[ExampleAdvisor])
            .build()
        }
      }

      "return two transformations" in {

        val transformations = ki.collectTransformations(moduleConfigurationMock, instrumentationMock)
        transformations.size shouldBe 1
        val transformation = transformations.get(0)
        transformation.getMixins.size() shouldBe 1
        transformation.getBridges.size() shouldBe 0
        transformation.getTransformations.size() shouldBe 1
        transformation.isActive shouldBe true
        transformation.getElementMatcher.isDefined shouldBe true

        verify(moduleConfigurationMock).shouldInjectInBootstrap()
        verify(moduleConfigurationMock).getTempDir
        verify(instrumentationMock).appendToBootstrapClassLoaderSearch(any())
        verifyNoMoreInteractions(moduleConfigurationMock)
        verifyNoMoreInteractions(instrumentationMock)

      }
    }
  }

}

trait ExampleMixin
trait ExampleAdvisor
