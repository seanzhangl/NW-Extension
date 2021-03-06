// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import scala.collection.immutable.HashSet

import org.nlogo.agent.Agent
import org.nlogo.agent.AgentSet
import org.nlogo.agent.ArrayAgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.TreeAgentSet
import org.nlogo.agent.Turtle
import org.nlogo.api.SimpleChangeEvent
import org.nlogo.api.SimpleChangeEventPublisher
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet

class AgentSetChangeSubscriber(agentSet: TreeAgentSet, onNotify: () => Unit)
  extends SimpleChangeEventPublisher#Sub {
  agentSet.simpleChangeEventPublisher.subscribe(this)
  def unsubscribe(): Unit = agentSet.simpleChangeEventPublisher.removeSubscription(this)
  override def notify(pub: SimpleChangeEventPublisher#Pub, event: SimpleChangeEvent.type) {
    onNotify.apply()
  }
}

trait MonitoredAgentSet[A <: Agent] {
  val graphContext: GraphContext
  def isValid(agent: A): Boolean
  def reset(): Unit
  def agentSet: AgentSet
}

trait MonitoredTreeAgentSet[A <: Agent] extends MonitoredAgentSet[A] {
  protected var initialAgentSet: TreeAgentSet
  val world = initialAgentSet.world
  val breedName = initialAgentSet.printName
  private var _agentSet: Option[AgentSet] = Some(initialAgentSet)
  initialAgentSet = null // don't hold on after initialization

  private def newChangeSubscriber: Option[AgentSetChangeSubscriber] =
    _agentSet.collect {
      case tas: TreeAgentSet =>
        new AgentSetChangeSubscriber(tas, graphContext.invalidate)
    }
  protected var changeSubscriber = newChangeSubscriber

  val UNBREEDED_NAME: String
  def unbreededAgentSet: AgentSet
  def getBreed(name: String): AgentSet
  def agentSet: AgentSet = _agentSet.getOrElse {
    val agentSet = breedName match {
      case UNBREEDED_NAME => unbreededAgentSet
      case name => Option(getBreed(name)).getOrElse {
        throw new IllegalArgumentException("Invalid breed name: " + name)
      }
    }
    _agentSet = Some(agentSet)
    changeSubscriber = newChangeSubscriber
    agentSet
  }
  override def reset(): Unit = {
    changeSubscriber.foreach(_.unsubscribe())
    changeSubscriber = None
    _agentSet = None
  }
}

class MonitoredTurtleTreeAgentSet(
  override var initialAgentSet: TreeAgentSet,
  override val graphContext: GraphContext)
  extends MonitoredTreeAgentSet[Turtle] {
  override val UNBREEDED_NAME: String = "TURTLES"
  override def unbreededAgentSet: AgentSet = world.turtles
  override def getBreed(name: String): AgentSet = world.getBreed(name)
  override def isValid(agent: Turtle): Boolean =
    (agentSet eq unbreededAgentSet) || (agentSet eq agent.getBreed)
}

class MonitoredLinkTreeAgentSet(
  override var initialAgentSet: TreeAgentSet,
  override val graphContext: GraphContext)
  extends MonitoredTreeAgentSet[Link] {
  override val UNBREEDED_NAME: String = "LINKS"
  override def unbreededAgentSet: AgentSet = world.links
  override def getBreed(name: String): AgentSet = world.getLinkBreed(name)
  override def isValid(agent: Link): Boolean =
    (agentSet eq unbreededAgentSet) || (agentSet eq agent.getBreed)
}

trait MonitoredArrayAgentSet[A <: Agent] extends MonitoredAgentSet[A] {
  def agentSet: ArrayAgentSet
  private var idSet: Set[Long] = _
  private var count: Int = _

  override def reset(): Unit = {
    idSet = HashSet.empty ++ agentSet.asIterable[A].map(_.id)
    count = idSet.size
  }
  reset()

  def verify(): Unit = {
    if (agentSet.count != count) {
      graphContext.invalidate()
    }
  }
  override def isValid(agent: A): Boolean = idSet.contains(agent.id)
}

class MonitoredTurtleArrayAgentSet(
  val agentSet: ArrayAgentSet,
  override val graphContext: GraphContext)
  extends MonitoredArrayAgentSet[Turtle] {
}

class MonitoredLinkArrayAgentSet(
  private val _agentSet: ArrayAgentSet,
  override val graphContext: GraphContext)
  extends MonitoredArrayAgentSet[Link] {
  def agentSet = {
    _agentSet
  }
}
