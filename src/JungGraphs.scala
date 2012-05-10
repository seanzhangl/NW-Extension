package org.nlogo.extensions.nw

import java.util.Collection

import scala.collection.JavaConverters.asJavaCollectionConverter

import org.nlogo.agent.AgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.util.Pair
import edu.uci.ics.jung.graph.DirectedGraph
import edu.uci.ics.jung.graph.UndirectedGraph
import edu.uci.ics.jung.graph.AbstractGraph
import edu.uci.ics.jung.graph.AbstractTypedGraph
import edu.uci.ics.jung.graph.Graph

object JungGraphUtil {
  implicit def EnrichNetLogoGraph(nlg: NetLogoGraph) = new RichNetLogoGraph(nlg)
  class RichNetLogoGraph(nlg: NetLogoGraph) {
    def asJungGraph = new UntypedJungGraph(nlg)
    def asDirectedJungGraph = new DirectedJungGraph(nlg)
    def asUndirectedJungGraph = new UndirectedJungGraph(nlg)
  }
}

abstract class JungGraph(val nlg: NetLogoGraph)
  extends AbstractGraph[Turtle, Link] {

  lazy val dijkstraShortestPath = new DijkstraShortestPath(this, nlg.isStatic)

  override def getInEdges(turtle: Turtle): Collection[Link] =
    nlg.validTurtle(turtle).map(nlg.inEdges(_).asJavaCollection).orNull
  override def getPredecessors(turtle: Turtle): Collection[Turtle] =
    nlg.validTurtle(turtle).map(nlg.inEdges(_).map(_.end1).asJavaCollection).orNull

  override def getOutEdges(turtle: Turtle): Collection[Link] =
    nlg.validTurtle(turtle).map(nlg.outEdges(_).asJavaCollection).orNull
  override def getSuccessors(turtle: Turtle): Collection[Turtle] =
    nlg.validTurtle(turtle).map(nlg.outEdges(_).map(_.end2).asJavaCollection).orNull

  override def getIncidentEdges(turtle: Turtle): Collection[Link] =
    nlg.validTurtle(turtle).map(nlg.edges(_).asJavaCollection).orNull

  override def getSource(link: Link): Turtle =
    nlg.validLink(link).filter(_.isDirectedLink).map(_.end1).orNull

  override def getDest(link: Link): Turtle =
    nlg.validLink(link).filter(_.isDirectedLink).map(_.end2).orNull

  override def getEdgeCount(): Int = nlg.links.size

  override def getNeighbors(turtle: Turtle): Collection[Turtle] =
    nlg.validTurtle(turtle).map { t =>
      (nlg.outEdges(t).map(_.end2) ++ nlg.inEdges(t).map(_.end1)).asJavaCollection
    }.orNull

  override def getVertexCount(): Int = nlg.turtles.size
  override def getVertices(): Collection[Turtle] = nlg.turtles.asJavaCollection
  override def getEdges(): Collection[Link] = nlg.links.asJavaCollection
  override def containsEdge(link: Link): Boolean = nlg.validLink(link).isDefined
  override def containsVertex(turtle: Turtle): Boolean = nlg.validTurtle(turtle).isDefined

  def getEndpoints(link: Link): Pair[Turtle] =
    new Pair(link.end1, link.end2) // Note: contract says nothing about edge being in graph

  def isDest(turtle: Turtle, link: Link): Boolean =
    nlg.validLink(link).filter(_.end2 == turtle).isDefined
  def isSource(turtle: Turtle, link: Link): Boolean =
    nlg.validLink(link).filter(_.end1 == turtle).isDefined

  // TODO: in a live graph, maybe they could be useful for generators
  def removeEdge(link: Link): Boolean =
    throw sys.error("not implemented")
  def removeVertex(turtle: Turtle): Boolean =
    throw sys.error("not implemented")
  def addVertex(turtle: Turtle): Boolean =
    throw sys.error("not implemented")
  override def addEdge(link: Link, turtles: Collection[_ <: Turtle]): Boolean =
    throw sys.error("not implemented")
  def addEdge(link: Link, turtles: Pair[_ <: Turtle], edgeType: EdgeType): Boolean =
    throw sys.error("not implemented")

}

class UntypedJungGraph(val nlg: NetLogoGraph)
  extends JungGraph(nlg) {
  override def getEdgeType(link: Link): EdgeType =
    if (link.isDirectedLink) EdgeType.DIRECTED else EdgeType.UNDIRECTED
  private def edges(edgeType: EdgeType) = nlg.links.filter(getEdgeType(_) == edgeType)
  override def getEdgeCount(edgeType: EdgeType) = edges(edgeType).size
  override def getEdges(edgeType: EdgeType) = edges(edgeType).asJavaCollection
  override def getDefaultEdgeType(): EdgeType = EdgeType.UNDIRECTED
}

class DirectedJungGraph(val nlg: NetLogoGraph)
  extends JungGraph(nlg)
  with DirectedGraph[Turtle, Link] {

  if (!nlg.isDirected)
    throw new ExtensionException("link set must be directed")

}

class UndirectedJungGraph(val nlg: NetLogoGraph)
  extends JungGraph(nlg)
  with UndirectedGraph[Turtle, Link] {
  if (!nlg.isUndirected)
    throw new ExtensionException("link set must be undirected")
}