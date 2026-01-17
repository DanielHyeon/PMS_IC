import { useMemo, useCallback } from 'react';
import {
  ReactFlow,
  Node,
  Edge,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  MarkerType,
  Position,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import dagre from '@dagrejs/dagre';
import { GitBranch } from 'lucide-react';
import { Badge } from '../ui/badge';
import {
  LineageGraphDto,
  LineageNodeDto,
  NODE_TYPE_CONFIG,
  LineageNodeType,
} from '../../../types/lineage';

interface LineageGraphProps {
  data: LineageGraphDto;
  onNodeClick?: (node: LineageNodeDto) => void;
}

const nodeWidth = 220;
const nodeHeight = 80;

// Layout nodes using dagre
function getLayoutedElements(
  nodes: Node[],
  edges: Edge[],
  direction: 'TB' | 'LR' = 'LR'
) {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));
  dagreGraph.setGraph({ rankdir: direction, nodesep: 50, ranksep: 100 });

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  const layoutedNodes = nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      },
    };
  });

  return { nodes: layoutedNodes, edges };
}

// Custom node component
function LineageNode({ data }: { data: { label: string; nodeType: LineageNodeType; status?: string; code?: string } }) {
  const config = NODE_TYPE_CONFIG[data.nodeType];

  return (
    <div
      className="px-4 py-3 rounded-lg border-2 bg-white shadow-sm min-w-[200px]"
      style={{ borderColor: config.color }}
    >
      <div className="flex items-center justify-between gap-2 mb-1">
        <Badge
          className="text-xs"
          style={{ backgroundColor: config.bgColor, color: config.color }}
        >
          {config.label}
        </Badge>
        {data.status && (
          <Badge variant="outline" className="text-xs">
            {data.status}
          </Badge>
        )}
      </div>
      {data.code && (
        <p className="text-xs font-mono text-gray-500 mb-0.5">{data.code}</p>
      )}
      <p className="text-sm font-medium text-gray-900 truncate">{data.label}</p>
    </div>
  );
}

const nodeTypes = {
  lineageNode: LineageNode,
};

export default function LineageGraph({ data, onNodeClick }: LineageGraphProps) {
  const { nodes: graphNodes, edges: graphEdges } = data;

  // Transform backend data to React Flow format
  const { initialNodes, initialEdges } = useMemo(() => {
    const rfNodes: Node[] = graphNodes.map((node) => ({
      id: node.id,
      type: 'lineageNode',
      position: { x: 0, y: 0 }, // Will be set by dagre
      data: {
        label: node.title,
        nodeType: node.type,
        status: node.status,
        code: node.code,
      },
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
    }));

    const rfEdges: Edge[] = graphEdges.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      type: 'smoothstep',
      animated: false,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 15,
        height: 15,
        color: '#94a3b8',
      },
      style: { stroke: '#94a3b8', strokeWidth: 2 },
    }));

    // Apply dagre layout
    const layouted = getLayoutedElements(rfNodes, rfEdges, 'LR');
    return { initialNodes: layouted.nodes, initialEdges: layouted.edges };
  }, [graphNodes, graphEdges]);

  const [nodes, , onNodesChange] = useNodesState(initialNodes);
  const [edges, , onEdgesChange] = useEdgesState(initialEdges);

  const handleNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      const originalNode = graphNodes.find((n) => n.id === node.id);
      if (originalNode && onNodeClick) {
        onNodeClick(originalNode);
      }
    },
    [graphNodes, onNodeClick]
  );

  if (graphNodes.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-[600px] text-gray-500">
        <GitBranch className="h-16 w-16 mb-4 text-gray-300" />
        <p className="text-lg font-medium">No lineage data available</p>
        <p className="text-sm">Create requirements and link them to tasks to see the lineage graph</p>
      </div>
    );
  }

  return (
    <div style={{ width: '100%', height: '600px' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        minZoom={0.1}
        maxZoom={2}
        defaultEdgeOptions={{
          type: 'smoothstep',
        }}
      >
        <Background color="#e5e7eb" gap={20} />
        <Controls />
        <MiniMap
          nodeColor={(node) => {
            const nodeType = node.data?.nodeType as LineageNodeType;
            return nodeType ? NODE_TYPE_CONFIG[nodeType]?.color || '#94a3b8' : '#94a3b8';
          }}
          maskColor="rgba(255, 255, 255, 0.8)"
        />
      </ReactFlow>
    </div>
  );
}
