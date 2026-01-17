package com.insuretech.pms.support;

import lombok.experimental.UtilityClass;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j Graph Data Factory for creating test graph structures.
 * Generates mock data for:
 * - Requirement nodes with properties
 * - UserStory nodes
 * - Task nodes
 * - Sprint nodes
 * - Relationships (DERIVES, BREAKS_DOWN_TO, BELONGS_TO)
 * - Vector embeddings for RAG
 *
 * All Korean data is provided for nodes while IDs remain in English/UUID.
 */
@UtilityClass
public class TestGraphDataFactory {

    // ============= Graph Node Models =============

    /**
     * Represents a Neo4j Requirement node
     */
    public static class RequirementNode {
        public String id;
        public String code;
        public String title;
        public String description;
        public String category;
        public String priority;
        public String status;
        public Integer progress;
        public String projectId;
        public List<Float> embedding;
        public String sourceText;

        public RequirementNode(String id, String code, String title, String description,
                              String category, String priority, String status, Integer progress,
                              String projectId, List<Float> embedding, String sourceText) {
            this.id = id;
            this.code = code;
            this.title = title;
            this.description = description;
            this.category = category;
            this.priority = priority;
            this.status = status;
            this.progress = progress;
            this.projectId = projectId;
            this.embedding = embedding;
            this.sourceText = sourceText;
        }
    }

    /**
     * Represents a Neo4j UserStory node
     */
    public static class UserStoryNode {
        public String id;
        public String title;
        public String description;
        public String priority;
        public Integer storyPoints;
        public String status;
        public String projectId;
        public List<Float> embedding;

        public UserStoryNode(String id, String title, String description, String priority,
                            Integer storyPoints, String status, String projectId, List<Float> embedding) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.storyPoints = storyPoints;
            this.status = status;
            this.projectId = projectId;
            this.embedding = embedding;
        }
    }

    /**
     * Represents a Neo4j Task node
     */
    public static class TaskNode {
        public String id;
        public String title;
        public String description;
        public String priority;
        public String status;
        public String trackType;
        public String projectId;

        public TaskNode(String id, String title, String description, String priority,
                       String status, String trackType, String projectId) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.status = status;
            this.trackType = trackType;
            this.projectId = projectId;
        }
    }

    /**
     * Represents a Neo4j Sprint node
     */
    public static class SprintNode {
        public String id;
        public String name;
        public String goal;
        public String status;
        public String projectId;

        public SprintNode(String id, String name, String goal, String status, String projectId) {
            this.id = id;
            this.name = name;
            this.goal = goal;
            this.status = status;
            this.projectId = projectId;
        }
    }

    /**
     * Represents a relationship between nodes
     */
    public static class GraphRelationship {
        public String fromNodeId;
        public String toNodeId;
        public String relationshipType;
        public Map<String, Object> properties;

        public GraphRelationship(String fromNodeId, String toNodeId, String relationshipType) {
            this(fromNodeId, toNodeId, relationshipType, new HashMap<>());
        }

        public GraphRelationship(String fromNodeId, String toNodeId, String relationshipType,
                                Map<String, Object> properties) {
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
            this.relationshipType = relationshipType;
            this.properties = properties;
        }
    }

    /**
     * Represents a vector embedding
     */
    public static class VectorEmbedding {
        public String nodeId;
        public List<Float> vector;
        public String model;
        public String text;
        public Integer dimension;

        public VectorEmbedding(String nodeId, List<Float> vector, String model, String text) {
            this.nodeId = nodeId;
            this.vector = vector;
            this.model = model;
            this.text = text;
            this.dimension = vector.size();
        }
    }

    // ============= Embedding Generators =============

    /**
     * Generates a deterministic mock embedding for testing.
     * Uses text hash to ensure reproducibility.
     */
    public static List<Float> generateMockEmbedding(String text, int dimension) {
        Random random = new Random(text.hashCode());
        List<Float> embedding = new ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            embedding.add(random.nextFloat() * 2 - 1); // Range: [-1, 1]
        }
        // Normalize
        float magnitude = (float) Math.sqrt(embedding.stream()
                .map(f -> f * f)
                .reduce(0.0f, (a, b) -> a + b));
        return embedding.stream()
                .map(f -> f / magnitude)
                .collect(Collectors.toList());
    }

    /**
     * Generates standard 384-dimensional embedding (typical for test models)
     */
    public static List<Float> generateMockEmbedding384(String text) {
        return generateMockEmbedding(text, 384);
    }

    // ============= Requirement Node Builders =============

    public static List<RequirementNode> createRequirementNodes(String projectId) {
        Object[][] requirementData = {
                {"REQ-AI-001", "AI 모델 정확도 90% 이상 달성", "머신러닝 모델의 보험심사 정확도를 90% 이상으로 개선하여 자동화 정확성 확보", "AI", "CRITICAL", "APPROVED", 60},
                {"REQ-AI-002", "실시간 심사 결과 제공", "사용자 요청 후 5분 이내에 심사 결과를 제공하는 실시간 처리 시스템", "AI", "HIGH", "IMPLEMENTED", 80},
                {"REQ-SEC-001", "고객 정보 암호화", "모든 고객 개인정보는 AES-256 암호화를 통해 보호하고 안전하게 저장", "SECURITY", "CRITICAL", "APPROVED", 75},
                {"REQ-FUNC-001", "모바일 청구 기능", "iOS 및 안드로이드 모바일 앱에서 보험청구 기능을 제공", "FUNCTIONAL", "HIGH", "ANALYZED", 40},
                {"REQ-FUNC-002", "조회 및 승인 프로세스", "고객이 청구 상태를 실시간으로 조회하고 승인 요청을 할 수 있음", "FUNCTIONAL", "MEDIUM", "IDENTIFIED", 20},
                {"REQ-INT-001", "기존 레거시 시스템 연동", "현재 운영 중인 보험 관리 시스템과 REST API를 통해 연동", "INTEGRATION", "HIGH", "ANALYZED", 50}
        };

        List<RequirementNode> nodes = new ArrayList<>();
        for (int i = 0; i < requirementData.length; i++) {
            Object[] data = requirementData[i];
            String nodeId = "req-" + (i + 1);
            String embedding384Text = (String) data[1] + " " + (String) data[2];
            RequirementNode node = new RequirementNode(
                    nodeId,
                    (String) data[0],
                    (String) data[1],
                    (String) data[2],
                    (String) data[3],
                    (String) data[4],
                    (String) data[5],
                    (Integer) data[6],
                    projectId,
                    generateMockEmbedding384(embedding384Text),
                    (String) data[2]
            );
            nodes.add(node);
        }
        return nodes;
    }

    // ============= UserStory Node Builders =============

    public static List<UserStoryNode> createUserStoryNodes(String projectId) {
        String[][] storyData = {
                {"AI 모델 훈련 파이프라인 구축", "머신러닝 모델을 자동으로 훈련하고 배포하는 파이프라인 시스템 개발", "HIGH", "8"},
                {"보험 청구 데이터 전처리", "원본 보험 청구 데이터를 머신러닝 학습에 적합하게 정제하고 전처리", "HIGH", "5"},
                {"모바일 앱 기본 UI 개발", "기본 로그인 및 메인 대시보드 UI를 구현하고 네비게이션 구조 완성", "HIGH", "5"},
                {"청구 상태 조회 기능", "고객이 자신의 청구 상태를 실시간으로 조회하고 알림 수신", "MEDIUM", "3"},
                {"보안 감사 및 취약점 분석", "OWASP Top 10 기준에 따른 보안 검증 및 취약점 분석 보고서 작성", "CRITICAL", "8"}
        };

        List<UserStoryNode> nodes = new ArrayList<>();
        for (int i = 0; i < storyData.length; i++) {
            String[] data = storyData[i];
            String nodeId = "story-" + (i + 1);
            String embedding384Text = data[0] + " " + data[1];
            UserStoryNode node = new UserStoryNode(
                    nodeId,
                    data[0],
                    data[1],
                    data[2],
                    Integer.parseInt(data[3]),
                    "BACKLOG",
                    projectId,
                    generateMockEmbedding384(embedding384Text)
            );
            nodes.add(node);
        }
        return nodes;
    }

    // ============= Task Node Builders =============

    public static List<TaskNode> createTaskNodes(String projectId) {
        String[][] taskData = {
                {"데이터셋 확보 및 검증", "학습에 필요한 보험 청구 데이터 수집 및 품질 검증", "HIGH", "TODO", "AI"},
                {"모델 파라미터 튜닝", "하이퍼파라미터 최적화를 통한 모델 성능 개선", "HIGH", "IN_PROGRESS", "AI"},
                {"API 엔드포인트 개발", "REST API 기반 예측 요청 인터페이스 구현", "MEDIUM", "REVIEW", "COMMON"},
                {"로깅 및 모니터링 설정", "프로덕션 환경에서의 성능 모니터링 구성", "MEDIUM", "TODO", "COMMON"},
                {"에러 처리 및 복구 로직", "시스템 오류 발생 시 적절한 예외 처리", "MEDIUM", "DONE", "COMMON"},
                {"보안 테스트 작성", "OWASP Top 10 기반 자동화된 보안 테스트 케이스", "CRITICAL", "IN_PROGRESS", "COMMON"},
                {"성능 최적화", "데이터베이스 쿼리 및 API 응답 시간 최적화", "HIGH", "TODO", "SI"},
                {"사용자 가이드 작성", "최종 사용자를 위한 시스템 사용 설명서 작성", "LOW", "TODO", "COMMON"}
        };

        List<TaskNode> nodes = new ArrayList<>();
        for (int i = 0; i < taskData.length; i++) {
            String[] data = taskData[i];
            String nodeId = "task-" + (i + 1);
            TaskNode node = new TaskNode(
                    nodeId,
                    data[0],
                    data[1],
                    data[2],
                    data[3],
                    data[4],
                    projectId
            );
            nodes.add(node);
        }
        return nodes;
    }

    // ============= Sprint Node Builders =============

    public static List<SprintNode> createSprintNodes(String projectId) {
        String[][] sprintData = {
                {"Sprint 1", "기본 AI 모델 구축 및 테스트", "PLANNED"},
                {"Sprint 2", "모델 정확도 개선 및 최적화", "PLANNED"},
                {"Sprint 3", "통합 테스트 및 배포 준비", "PLANNED"}
        };

        List<SprintNode> nodes = new ArrayList<>();
        for (int i = 0; i < sprintData.length; i++) {
            String[] data = sprintData[i];
            String nodeId = "sprint-" + (i + 1);
            SprintNode node = new SprintNode(
                    nodeId,
                    data[0],
                    data[1],
                    data[2],
                    projectId
            );
            nodes.add(node);
        }
        return nodes;
    }

    // ============= Graph Relationship Builders =============

    /**
     * Creates lineage relationships between nodes.
     * Requirement -> UserStory -> Task -> Sprint
     */
    public static List<GraphRelationship> createLineageRelationships(
            List<RequirementNode> requirements,
            List<UserStoryNode> stories,
            List<TaskNode> tasks,
            List<SprintNode> sprints) {

        List<GraphRelationship> relationships = new ArrayList<>();

        // Requirement -> UserStory (DERIVES relationship)
        // REQ-AI-001 -> Story1, Story2
        // REQ-AI-002 -> Story1
        // REQ-SEC-001 -> Story5
        // REQ-FUNC-001 -> Story3, Story4
        // REQ-FUNC-002 -> Story4
        // REQ-INT-001 -> Story1
        relationships.add(new GraphRelationship(requirements.get(0).id, stories.get(0).id, "DERIVES"));
        relationships.add(new GraphRelationship(requirements.get(0).id, stories.get(1).id, "DERIVES"));
        relationships.add(new GraphRelationship(requirements.get(1).id, stories.get(0).id, "DERIVES"));
        relationships.add(new GraphRelationship(requirements.get(2).id, stories.get(4).id, "DERIVES"));
        relationships.add(new GraphRelationship(requirements.get(3).id, stories.get(2).id, "DERIVES"));
        relationships.add(new GraphRelationship(requirements.get(3).id, stories.get(3).id, "DERIVES"));
        relationships.add(new GraphRelationship(requirements.get(4).id, stories.get(3).id, "DERIVES"));
        relationships.add(new GraphRelationship(requirements.get(5).id, stories.get(0).id, "DERIVES"));

        // UserStory -> Task (BREAKS_DOWN_TO relationship)
        // Story1 -> Task1, Task2, Task3
        // Story2 -> Task2, Task7
        // Story3 -> Task3, Task4
        // Story4 -> Task1, Task4, Task8
        // Story5 -> Task6
        relationships.add(new GraphRelationship(stories.get(0).id, tasks.get(0).id, "BREAKS_DOWN_TO"));
        relationships.add(new GraphRelationship(stories.get(0).id, tasks.get(1).id, "BREAKS_DOWN_TO"));
        relationships.add(new GraphRelationship(stories.get(0).id, tasks.get(2).id, "BREAKS_DOWN_TO"));

        relationships.add(new GraphRelationship(stories.get(1).id, tasks.get(1).id, "BREAKS_DOWN_TO"));
        relationships.add(new GraphRelationship(stories.get(1).id, tasks.get(6).id, "BREAKS_DOWN_TO"));

        relationships.add(new GraphRelationship(stories.get(2).id, tasks.get(2).id, "BREAKS_DOWN_TO"));
        relationships.add(new GraphRelationship(stories.get(2).id, tasks.get(3).id, "BREAKS_DOWN_TO"));

        relationships.add(new GraphRelationship(stories.get(3).id, tasks.get(0).id, "BREAKS_DOWN_TO"));
        relationships.add(new GraphRelationship(stories.get(3).id, tasks.get(3).id, "BREAKS_DOWN_TO"));
        relationships.add(new GraphRelationship(stories.get(3).id, tasks.get(7).id, "BREAKS_DOWN_TO"));

        relationships.add(new GraphRelationship(stories.get(4).id, tasks.get(5).id, "BREAKS_DOWN_TO"));

        // Task -> Sprint (BELONGS_TO relationship)
        // Task1, Task2, Task3 -> Sprint1
        // Task4, Task5 -> Sprint2
        // Task6, Task7, Task8 -> Sprint3
        relationships.add(new GraphRelationship(tasks.get(0).id, sprints.get(0).id, "BELONGS_TO"));
        relationships.add(new GraphRelationship(tasks.get(1).id, sprints.get(0).id, "BELONGS_TO"));
        relationships.add(new GraphRelationship(tasks.get(2).id, sprints.get(0).id, "BELONGS_TO"));

        relationships.add(new GraphRelationship(tasks.get(3).id, sprints.get(1).id, "BELONGS_TO"));
        relationships.add(new GraphRelationship(tasks.get(4).id, sprints.get(1).id, "BELONGS_TO"));

        relationships.add(new GraphRelationship(tasks.get(5).id, sprints.get(2).id, "BELONGS_TO"));
        relationships.add(new GraphRelationship(tasks.get(6).id, sprints.get(2).id, "BELONGS_TO"));
        relationships.add(new GraphRelationship(tasks.get(7).id, sprints.get(2).id, "BELONGS_TO"));

        return relationships;
    }

    // ============= Vector Embeddings =============

    public static List<VectorEmbedding> createRequirementEmbeddings(List<RequirementNode> requirements) {
        List<VectorEmbedding> embeddings = new ArrayList<>();
        for (RequirementNode req : requirements) {
            embeddings.add(new VectorEmbedding(
                    req.id,
                    req.embedding,
                    "test-embedding-v1",
                    req.title + " " + req.description
            ));
        }
        return embeddings;
    }

    public static List<VectorEmbedding> createUserStoryEmbeddings(List<UserStoryNode> stories) {
        List<VectorEmbedding> embeddings = new ArrayList<>();
        for (UserStoryNode story : stories) {
            embeddings.add(new VectorEmbedding(
                    story.id,
                    story.embedding,
                    "test-embedding-v1",
                    story.title + " " + story.description
            ));
        }
        return embeddings;
    }

    // ============= Complete Graph Structure =============

    /**
     * Creates a complete graph structure for a project with all nodes and relationships
     */
    public static class GraphStructure {
        public List<RequirementNode> requirements;
        public List<UserStoryNode> userStories;
        public List<TaskNode> tasks;
        public List<SprintNode> sprints;
        public List<GraphRelationship> relationships;
        public List<VectorEmbedding> embeddings;

        public GraphStructure(String projectId) {
            this.requirements = createRequirementNodes(projectId);
            this.userStories = createUserStoryNodes(projectId);
            this.tasks = createTaskNodes(projectId);
            this.sprints = createSprintNodes(projectId);
            this.relationships = createLineageRelationships(
                    this.requirements,
                    this.userStories,
                    this.tasks,
                    this.sprints
            );
            this.embeddings = new ArrayList<>();
            this.embeddings.addAll(createRequirementEmbeddings(this.requirements));
            this.embeddings.addAll(createUserStoryEmbeddings(this.userStories));
        }

        public int getTotalNodeCount() {
            return requirements.size() + userStories.size() + tasks.size() + sprints.size();
        }

        public int getTotalRelationshipCount() {
            return relationships.size();
        }

        public int getTotalEmbeddingCount() {
            return embeddings.size();
        }
    }

    /**
     * Creates a complete graph structure for testing
     */
    public static GraphStructure createCompleteGraphStructure(String projectId) {
        return new GraphStructure(projectId);
    }

    // ============= Cypher Query Generators =============

    /**
     * Generates Cypher script for creating all graph data
     */
    public static String generateCypherScript(GraphStructure structure) {
        StringBuilder cypher = new StringBuilder();

        // Create Requirement nodes
        cypher.append("// Create Requirement nodes\n");
        for (RequirementNode req : structure.requirements) {
            cypher.append(String.format(
                    "CREATE (req_%s:Requirement {id: '%s', code: '%s', title: '%s', category: '%s', " +
                    "priority: '%s', status: '%s', progress: %d, projectId: '%s'})\n",
                    req.id.replace("-", "_"), req.id, req.code, req.title, req.category,
                    req.priority, req.status, req.progress, req.projectId
            ));
        }

        // Create UserStory nodes
        cypher.append("\n// Create UserStory nodes\n");
        for (UserStoryNode story : structure.userStories) {
            cypher.append(String.format(
                    "CREATE (story_%s:UserStory {id: '%s', title: '%s', priority: '%s', " +
                    "storyPoints: %d, status: '%s', projectId: '%s'})\n",
                    story.id.replace("-", "_"), story.id, story.title, story.priority,
                    story.storyPoints, story.status, story.projectId
            ));
        }

        // Create Task nodes
        cypher.append("\n// Create Task nodes\n");
        for (TaskNode task : structure.tasks) {
            cypher.append(String.format(
                    "CREATE (task_%s:Task {id: '%s', title: '%s', priority: '%s', " +
                    "status: '%s', trackType: '%s', projectId: '%s'})\n",
                    task.id.replace("-", "_"), task.id, task.title, task.priority,
                    task.status, task.trackType, task.projectId
            ));
        }

        // Create Sprint nodes
        cypher.append("\n// Create Sprint nodes\n");
        for (SprintNode sprint : structure.sprints) {
            cypher.append(String.format(
                    "CREATE (sprint_%s:Sprint {id: '%s', name: '%s', goal: '%s', " +
                    "status: '%s', projectId: '%s'})\n",
                    sprint.id.replace("-", "_"), sprint.id, sprint.name, sprint.goal,
                    sprint.status, sprint.projectId
            ));
        }

        // Create relationships
        cypher.append("\n// Create relationships\n");
        for (GraphRelationship rel : structure.relationships) {
            cypher.append(String.format(
                    "MATCH (a {id: '%s'}), (b {id: '%s'}) CREATE (a)-[:%s]->(b)\n",
                    rel.fromNodeId, rel.toNodeId, rel.relationshipType
            ));
        }

        return cypher.toString();
    }

    /**
     * Generates Cypher for similarity search
     */
    public static String generateSimilaritySearchCypher(String queryEmbedding, float threshold) {
        return String.format(
                "MATCH (n:Requirement) WHERE n.embedding IS NOT NULL " +
                "AND gds.similarity.cosine(n.embedding, %s) > %f " +
                "RETURN n.id, n.title, n.category, gds.similarity.cosine(n.embedding, %s) AS similarity " +
                "ORDER BY similarity DESC LIMIT 10",
                queryEmbedding, threshold, queryEmbedding
        );
    }
}
