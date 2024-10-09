package gay.nyako.nextech.network;

import com.google.common.graph.*;
import gay.nyako.nextech.NexTech;
import gay.nyako.nextech.block.AbstractPipeBlockEntity;
import gay.nyako.nextech.network.node.PipeNetworkNode;
import gay.nyako.nextech.network.node.PipeNetworkNodeProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class PipeNetwork {
    public static final Identifier ITEMS = Identifier.of("nextech", "items");
    public static final Identifier FLUIDS = Identifier.of("nextech", "fluids");
    public static final Identifier ENERGY = Identifier.of("nextech", "energy");

    public static PipeNetwork getInstance(Identifier networkId, World world) {
        return PipeNetworkManager.getInstance(world).getPipeNetwork(networkId);
    }

    private final World world;
    private final Identifier networkId;

    private final MutableGraph<PipeNetworkNode> nodeGraph;

    private final HashMap<PipeNetworkNode, ArrayList<NodeWithDistance<PipeNetworkNode>>> nodeDestinations;
    private final HashMap<PipeNetworkNode, HashSet<PipeNetworkNode>> nodeSources;

    private final HashSet<PipeNetworkNode> nodes;
    private final HashSet<PipeNetworkNode> dirtyNodes;
    private final HashSet<PipeNetworkNode> dirtyNodesTemp;
    private final HashSet<PipeNetworkNode> tickableNodes;

    private final Deque<AbstractPipeBlockEntity> loadQueue;

    public PipeNetwork(Identifier networkId, World world) {
        this.world = world;
        this.networkId = networkId;

        nodeGraph = GraphBuilder.undirected().build();

        nodeDestinations = new HashMap<>();
        nodeSources = new HashMap<>();

        nodes = new HashSet<>();
        dirtyNodes = new HashSet<>();
        dirtyNodesTemp = new HashSet<>();
        tickableNodes = new HashSet<>();

        loadQueue = new ArrayDeque<>();
    }

    public void tick(BigInteger tickCount) {
        if (!loadQueue.isEmpty()) {
            NexTech.LOGGER.info("Floodfilling networks");
        }

        while (!loadQueue.isEmpty()) {
            var pipe = loadQueue.pop();

            if (pipe.initializedConnections()) {
                continue;
            }

            pipe.updateConnections();

            var node = pipe.getPipeNetworkNode();

            if (nodes.contains(node)) {
                for (var connectedNode : nodeGraph.successors(node)) {
                    if (!connectedNode.isEndpoint()) {
                        var connectedBlockEntity = world.getBlockEntity(connectedNode.getPipePos());

                        if (connectedBlockEntity instanceof AbstractPipeBlockEntity connectedPipe && !connectedPipe.initializedConnections()) {
                            loadQueue.push(connectedPipe);
                        }
                    }
                }
            }
        }

        if (!dirtyNodesTemp.isEmpty()) {
            NexTech.LOGGER.info("Network endpoints were updated this tick");
            dirtyNodesTemp.clear();
        }

        var tickableNodesCopy = List.copyOf(tickableNodes);

        //NexTech.LOGGER.info("Ticking {}", tickableNodes.size());

        for (var node : tickableNodesCopy) {
            var pos = node.getPipePos();
            var side = node.getPipeSide();

            var chunkPos = new ChunkPos(pos);
            if (world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                var pipe = node.getPipeBlockEntity(world);

                pipe.tickNetwork(this, node, side, tickCount);
            }
        }
    }

    public void queueForLoad(AbstractPipeBlockEntity pipeBlockEntity) {
        loadQueue.push(pipeBlockEntity);
    }

    public boolean containsNode(PipeNetworkNodeProvider node) {
        return nodes.contains(node.getPipeNetworkNode());
    }

    public void addNode(PipeNetworkNodeProvider node) {
        var actualNode = node.getPipeNetworkNode();

        if (!nodeGraph.addNode(actualNode)) {
            return;
        }

        nodes.add(actualNode);

        if (!actualNode.isEndpoint()) {
            return;
        }

        // Endpoint behaviour

        if (actualNode.shouldTick()) {
            tickableNodes.add(actualNode);
        }

        dirtyNodes.add(actualNode);
    }

    public void removeNode(PipeNetworkNodeProvider node) {
        var actualNode = node.getPipeNetworkNode();

        if (!nodes.contains(actualNode)) {
            return;
        }

        var removedSuccessors = new ArrayList<PipeNetworkNode>();

        for (var successor : nodeGraph.successors(actualNode)) {
            if (successor != actualNode && nodeGraph.degree(successor) <= 1) {
                removedSuccessors.add(successor);
            }
        }

        nodes.remove(actualNode);
        nodeGraph.removeNode(actualNode);

        for (var successor : removedSuccessors) {
            removeNode(successor);
        }

        if (!actualNode.isEndpoint()) {
            return;
        }

        // Endpoint behaviour

        if (nodeDestinations.containsKey(actualNode)) {
            for (var destination : nodeDestinations.get(actualNode)) {
                var destinationNode = destination.node();

                if (nodeSources.containsKey(destinationNode)) {
                    nodeSources.get(destinationNode).remove(actualNode);
                }
            }
        }

        nodeDestinations.remove(actualNode);
        nodeSources.remove(actualNode);

        if (actualNode.shouldTick()) {
            tickableNodes.remove(actualNode);
        }

        dirtyNodes.remove(actualNode);
    }

    public void addConnection(PipeNetworkNodeProvider source, PipeNetworkNodeProvider destination) {
        var sourceNode = source.getPipeNetworkNode();
        var destinationNode = destination.getPipeNetworkNode();

        addNode(sourceNode);
        addNode(destinationNode);

        nodeGraph.putEdge(sourceNode, destinationNode);

        markDirty(sourceNode, true);
    }

    public void removeConnection(PipeNetworkNodeProvider source, PipeNetworkNodeProvider destination) {
        var sourceNode = source.getPipeNetworkNode();
        var destinationNode = destination.getPipeNetworkNode();

        nodeGraph.removeEdge(sourceNode, destinationNode);

        if (nodeGraph.degree(sourceNode) == 0) {
            removeNode(sourceNode);
        } else {
            markDirty(sourceNode, true);
        }

        if (nodeGraph.degree(destinationNode) == 0) {
            removeNode(destinationNode);
        }
    }

    public void markDirty(PipeNetworkNodeProvider node, boolean includeSelf) {
        var actualNode = node.getPipeNetworkNode();

        if (actualNode.isEndpoint()) {
            if (includeSelf) {
                dirtyNodes.add(actualNode);
            }

            if (nodeSources.containsKey(actualNode)) {
                dirtyNodes.addAll(nodeSources.get(actualNode));
            }
        } else {
            var search = breadthFirstWithDistance(nodeGraph, actualNode, (n) -> !dirtyNodesTemp.contains(n.node()), true);

            for (var searchResult : search) {
                var searchNode = searchResult.node();

                dirtyNodesTemp.add(searchNode);

                if (searchNode.isEndpoint()) {
                    dirtyNodes.add(searchNode);
                }
            }
        }
    }

    public void updateDestinations(PipeNetworkNodeProvider node) {
        var actualNode = node.getPipeNetworkNode();

        if (!dirtyNodes.contains(actualNode)) {
            return;
        }

        dirtyNodes.remove(actualNode);

        if (nodeDestinations.containsKey(actualNode)) {
            for (var destination : nodeDestinations.get(actualNode)) {
                var destinationNode = destination.node();

                if (nodeSources.containsKey(destinationNode)) {
                    nodeSources.get(destinationNode).remove(actualNode);
                }
            }
        }

        var newDestinations = new ArrayList<NodeWithDistance<PipeNetworkNode>>();

        for (var endpoint : getEndpointsFrom(actualNode)) {
            var destinationNode = endpoint.node();

            if (destinationNode.getConnectionMode() == ConnectionMode.OUT || destinationNode == node) {
                continue;
            }

            newDestinations.add(endpoint);

            if (!nodeSources.containsKey(destinationNode)) {
                nodeSources.put(destinationNode, new HashSet<>());
            }

            nodeSources.get(destinationNode).add(actualNode);
        }

        newDestinations.sort(Comparator.comparingInt(NodeWithDistance::distance));

        nodeDestinations.put(actualNode, newDestinations);
    }

    public List<NodeWithDistance<PipeNetworkNode>> getDestinations(PipeNetworkNodeProvider node) {
        var actualNode = node.getPipeNetworkNode();

        updateDestinations(actualNode);

        return nodeDestinations.getOrDefault(actualNode, new ArrayList<>());
    }

    public List<NodeWithDistance<PipeNetworkNode>> getEndpointsFrom(PipeNetworkNode node) {
        return getEndpointsFrom(node, (n) -> true, false);
    }

    private List<NodeWithDistance<PipeNetworkNode>> getEndpointsFrom(PipeNetworkNode node, Predicate<NodeWithDistance<PipeNetworkNode>> filter, boolean blocking) {
        var result = new ArrayList<NodeWithDistance<PipeNetworkNode>>();

        if (!containsNode(node)) {
            return result;
        }

        for (var search : breadthFirstWithDistance(nodeGraph, node, filter, blocking)) {
            if (search.node().isEndpoint()) {
                result.add(search);
            }
        }

        return result;
    }

    private static <N> Iterable<NodeWithDistance<N>> breadthFirstWithDistance(SuccessorsFunction<N> graph, N startNode) {
        return breadthFirstWithDistance(graph, startNode, (n) -> true, false);
    }

    private static <N> Iterable<NodeWithDistance<N>> breadthFirstWithDistance(SuccessorsFunction<N> graph, N startNode, Predicate<NodeWithDistance<N>> filter, boolean blocking) {
        var queue = new ArrayDeque<NodeWithDistance<N>>();
        var visited = new HashSet<N>();

        for (N node : graph.successors(startNode)) {
            if (visited.contains(node)) {
                continue;
            }
            var nodeDist = new NodeWithDistance<>(node, 1);
            if (!blocking || filter.test(nodeDist)) {
                queue.add(nodeDist);
            }
            visited.add(node);
        }

        var iterator = new Iterator<NodeWithDistance<N>>() {
            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public NodeWithDistance<N> next() {
                NodeWithDistance<N> element;

                do {
                    element = queue.poll();

                    for (N node : graph.successors(element.node())) {
                        if (visited.contains(node)) {
                            continue;
                        }
                        var nodeDist = new NodeWithDistance<>(node, element.distance() + 1);
                        if (!blocking || filter.test(nodeDist)) {
                            queue.add(nodeDist);
                        }
                        visited.add(node);
                    }
                } while (!blocking && !filter.test(element));

                return element;
            }
        };

        return new Iterable<NodeWithDistance<N>>() {
            @Override
            public @NotNull Iterator<NodeWithDistance<N>> iterator() {
                return iterator;
            }
        };
    }
}
