package kamon.agent.scala

import java.lang.instrument.Instrumentation

import kamon.agent.util.conf.AgentConfiguration
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.mockito.Mockito._

class KamonInstrumentationSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  "a KamonInstrumentation from agent-scala-extension" when {
    "instrumenting with a single mixin" should {

      "return a single transformation" in {

        val instrumentationMock = mock(classOf[Instrumentation])
        val moduleConfigurationMock = mock(classOf[AgentConfiguration.ModuleConfiguration])
        when(moduleConfigurationMock.shouldInjectInBootstrap()).thenReturn(false)

        val ki = new KamonInstrumentation {
          forSubtypeOf("laala") { builder ⇒
            builder
              .withMixin(classOf[ExampleMixin])
              .build()
          }
        }

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
  }

}

trait ExampleMixin
