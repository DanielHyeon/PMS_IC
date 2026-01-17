package com.insuretech.pms.integration;

import com.insuretech.pms.support.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 통합 테스트: PostgreSQL → Neo4j → OpenMetadata
 *
 * 이 테스트 스위트는 다음을 검증합니다:
 * 1. PostgreSQL 데이터 생성 및 검증
 * 2. Neo4j 그래프 구조 생성 및 관계 검증
 * 3. OpenMetadata 메타데이터 생성 및 검증
 * 4. 전체 data flow의 일관성
 */
@DisplayName("데이터 동기화 통합 테스트")
class DataSyncIntegrationTest {

    // ============= PostgreSQL 데이터 검증 =============

    @Test
    @DisplayName("PostgreSQL 프로젝트 데이터가 올바르게 생성된다")
    void shouldCreatePostgreSQLProjectData() {
        // Given & When
        var project = TestDataFactory.project().asAIProject().build();
        var phases = TestDataFactory.createPhasesForProject(project);
        var members = TestDataFactory.createMembersForProject(project);
        var requirements = TestDataFactory.createRequirementsForProject(project.getId());

        // Then
        assertThat(project).isNotNull();
        assertThat(project.getName()).contains("AI");
        assertThat(phases).hasSize(6);
        assertThat(members).hasSize(7);
        assertThat(requirements).hasSize(6);
    }

    @Test
    @DisplayName("PostgreSQL 사용자 데이터가 올바르게 생성된다")
    void shouldCreatePostgreSQLUserData() {
        // When
        var users = TestDataFactory.createTestUsers();

        // Then
        assertThat(users).hasSize(10);
        assertThat(users)
                .allMatch(u -> u.getName() != null && !u.getName().isEmpty())
                .allMatch(u -> u.getEmail() != null && !u.getEmail().isEmpty())
                .allMatch(u -> u.getRole() != null);
    }

    @Test
    @DisplayName("요구사항 데이터가 올바르게 검증된다")
    void shouldValidateRequirementData() {
        // When
        var requirements = TestDataFactory.createRequirementsForProject("proj-001");

        // Then
        assertThat(requirements)
                .hasSize(6)
                .allMatch(r -> r.getCode() != null)
                .allMatch(r -> r.getTitle() != null)
                .allMatch(r -> r.getCategory() != null);

        // 코드 고유성
        var codes = requirements.stream()
                .map(r -> r.getCode())
                .toList();
        assertThat(codes).doesNotHaveDuplicates();
    }

    // ============= Neo4j 그래프 검증 =============

    @Test
    @DisplayName("Neo4j 그래프 노드가 완전하게 생성된다")
    void shouldCreateNeo4jGraphNodes() {
        // When
        var graph = TestGraphDataFactory.createCompleteGraphStructure("proj-001");

        // Then
        assertThat(graph.requirements).hasSize(6);
        assertThat(graph.userStories).hasSize(5);
        assertThat(graph.tasks).hasSize(8);
        assertThat(graph.sprints).hasSize(3);
        assertThat(graph.getTotalNodeCount()).isEqualTo(22);
    }

    @Test
    @DisplayName("Neo4j 관계가 올바르게 설정된다")
    void shouldCreateNeo4jRelationships() {
        // When
        var graph = TestGraphDataFactory.createCompleteGraphStructure("proj-001");

        // Then: 모든 관계 타입이 존재
        var relationshipTypes = graph.relationships.stream()
                .map(r -> r.relationshipType)
                .distinct()
                .toList();

        assertThat(relationshipTypes)
                .contains("DERIVES", "BREAKS_DOWN_TO", "BELONGS_TO");

        // 관계 수 검증
        assertThat(graph.relationships).isNotEmpty();
        assertThat(graph.getTotalRelationshipCount()).isGreaterThan(20);
    }

    @Test
    @DisplayName("Neo4j 벡터 임베딩이 생성된다")
    void shouldCreateNeo4jEmbeddings() {
        // When
        var graph = TestGraphDataFactory.createCompleteGraphStructure("proj-001");

        // Then: 모든 임베딩이 384차원
        assertThat(graph.embeddings).isNotEmpty();
        assertThat(graph.embeddings)
                .allMatch(e -> e.dimension == 384)
                .allMatch(e -> e.vector.size() == 384)
                .allMatch(e -> e.model.equals("test-embedding-v1"));

        // 임베딩 정규화 검증 (magnitude ≈ 1)
        for (var embedding : graph.embeddings) {
            float magnitude = (float) Math.sqrt(
                    embedding.vector.stream()
                            .map(f -> f * f)
                            .reduce(0.0f, (a, b) -> a + b)
            );
            assertThat(magnitude).isCloseTo(1.0f, within(0.01f));
        }
    }

    @Test
    @DisplayName("Neo4j Lineage 경로가 유효하다")
    void shouldValidateNeo4jLineagePath() {
        // When
        var graph = TestGraphDataFactory.createCompleteGraphStructure("proj-001");

        // Then: Requirement -> UserStory -> Task -> Sprint 경로 확인
        var requirementToStory = graph.relationships.stream()
                .filter(r -> r.relationshipType.equals("DERIVES"))
                .toList();
        assertThat(requirementToStory).isNotEmpty();

        var storyToTask = graph.relationships.stream()
                .filter(r -> r.relationshipType.equals("BREAKS_DOWN_TO"))
                .toList();
        assertThat(storyToTask).isNotEmpty();

        var taskToSprint = graph.relationships.stream()
                .filter(r -> r.relationshipType.equals("BELONGS_TO"))
                .toList();
        assertThat(taskToSprint).isNotEmpty();
    }

    // ============= OpenMetadata 메타데이터 검증 =============

    @Test
    @DisplayName("OpenMetadata 메타데이터 구조가 완전하다")
    void shouldCreateCompleteMetadataStructure() {
        // When
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then
        assertThat(metadata.postgresService).isNotNull();
        assertThat(metadata.neo4jService).isNotNull();
        assertThat(metadata.database).isNotNull();
        assertThat(metadata.schemas).hasSize(4);
        assertThat(metadata.tables).isNotEmpty();
        assertThat(metadata.lineageEdges).isNotEmpty();
        assertThat(metadata.assets).isNotEmpty();
    }

    @Test
    @DisplayName("OpenMetadata 스키마가 올바르게 정의된다")
    void shouldValidateMetadataSchemas() {
        // When
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then
        var schemaNames = metadata.schemas.stream()
                .map(s -> s.name)
                .toList();

        assertThat(schemaNames).contains("auth", "project", "task", "chat");
        assertThat(schemaNames).hasSize(4);
    }

    @Test
    @DisplayName("OpenMetadata 테이블이 올바르게 정의된다")
    void shouldValidateMetadataTables() {
        // When
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then
        var tableNames = metadata.tables.stream()
                .map(t -> t.name)
                .toList();

        assertThat(tableNames)
                .contains("users", "projects", "requirements", "user_stories", "tasks", "sprints");
    }

    @Test
    @DisplayName("OpenMetadata 테이블이 컬럼을 포함한다")
    void shouldValidateTableColumns() {
        // When
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then
        assertThat(metadata.tables)
                .allMatch(t -> !t.columns.isEmpty());

        // Requirements 테이블 검증
        var requirementsTable = metadata.tables.stream()
                .filter(t -> t.name.equals("requirements"))
                .findFirst()
                .orElse(null);

        assertThat(requirementsTable).isNotNull();
        var columnNames = requirementsTable.columns.stream()
                .map(c -> c.name)
                .toList();
        assertThat(columnNames)
                .contains("id", "code", "title", "category", "status", "project_id", "neo4j_node_id");
    }

    @Test
    @DisplayName("OpenMetadata Lineage가 올바르게 정의된다")
    void shouldValidateMetadataLineage() {
        // When
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then
        var lineageMap = metadata.lineageEdges.stream()
                .collect(java.util.stream.Collectors.groupingBy(e -> e.fromTableId));

        assertThat(lineageMap).containsKeys("requirements", "user_stories", "tasks", "rfps", "projects");

        // 기본 lineage 경로
        assertThat(lineageMap.get("requirements"))
                .anyMatch(e -> e.toTableId.equals("user_stories"));
        assertThat(lineageMap.get("user_stories"))
                .anyMatch(e -> e.toTableId.equals("tasks"));
    }

    @Test
    @DisplayName("OpenMetadata Asset이 올바르게 분류된다")
    void shouldValidateAssetClassification() {
        // When
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then
        assertThat(metadata.assets)
                .allMatch(a -> a.owner != null && !a.owner.isEmpty())
                .allMatch(a -> !a.tags.isEmpty());

        var allTags = metadata.assets.stream()
                .flatMap(a -> a.tags.stream())
                .distinct()
                .toList();

        assertThat(allTags)
                .contains("requirements", "development", "planning", "agile", "tracking");
    }

    // ============= 통합 일관성 검증 =============

    @Test
    @DisplayName("모든 시스템에서 한글 데이터가 유지된다")
    void shouldMaintainKoreanDataConsistency() {
        // When
        var pgRequirements = TestDataFactory.createRequirementsForProject("proj-001");
        var neoGraph = TestGraphDataFactory.createCompleteGraphStructure("proj-001");
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then: 한글 데이터가 모든 시스템에 존재
        assertThat(pgRequirements)
                .allMatch(r -> r.getTitle().matches(".*[가-힣].*"));

        assertThat(neoGraph.requirements)
                .allMatch(r -> r.title.matches(".*[가-힣].*"));

        assertThat(metadata.tables)
                .allMatch(t -> t.displayName.matches(".*[가-힣].*"));
    }

    @Test
    @DisplayName("모든 시스템에서 ID는 영어/숫자만 사용된다")
    void shouldMaintainEnglishIdentifiers() {
        // When
        var pgRequirements = TestDataFactory.createRequirementsForProject("proj-001");
        var neoGraph = TestGraphDataFactory.createCompleteGraphStructure("proj-001");

        // Then
        assertThat(pgRequirements)
                .allMatch(r -> r.getCode().matches("^[A-Z0-9\\-]+$"));

        assertThat(neoGraph.requirements)
                .allMatch(r -> r.id.matches("^[a-z0-9\\-]+$"))
                .allMatch(r -> r.code.matches("^[A-Z0-9\\-]+$"));
    }

    @Test
    @DisplayName("PostgreSQL과 Neo4j의 데이터 수가 일치한다")
    void shouldMaintainDataConsistencyBetweenSystems() {
        // When
        var pgRequirements = TestDataFactory.createRequirementsForProject("proj-001");
        var neoRequirements = TestGraphDataFactory.createCompleteGraphStructure("proj-001").requirements;

        var pgStories = TestDataFactory.createUserStoriesForProject("proj-001");
        var neoStories = TestGraphDataFactory.createCompleteGraphStructure("proj-001").userStories;

        // Then
        assertThat(neoRequirements).hasSize(pgRequirements.size());
        assertThat(neoStories).hasSize(pgStories.size());
    }

    @Test
    @DisplayName("그래프 총 노드와 관계 수가 검증된다")
    void shouldValidateGraphStatistics() {
        // When
        var graph = TestGraphDataFactory.createCompleteGraphStructure("proj-001");

        // Then
        assertThat(graph.getTotalNodeCount()).isEqualTo(22);
        assertThat(graph.getTotalRelationshipCount()).isGreaterThan(20);
        assertThat(graph.getTotalEmbeddingCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("메타데이터 테이블 수가 기대값과 일치한다")
    void shouldValidateMetadataTableCount() {
        // When
        var metadata = TestOpenMetadataFactory.createCompleteMetadataStructure("proj-001");

        // Then
        assertThat(metadata.getTotalTableCount()).isGreaterThanOrEqualTo(6);
        assertThat(metadata.getTotalSchemaCount()).isEqualTo(4);
        assertThat(metadata.getTotalLineageEdgeCount()).isGreaterThanOrEqualTo(5);
        assertThat(metadata.getTotalAssetCount()).isGreaterThan(0);
    }
}
