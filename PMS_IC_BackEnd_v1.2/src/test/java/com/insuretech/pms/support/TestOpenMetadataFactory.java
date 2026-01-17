package com.insuretech.pms.support;

import lombok.experimental.UtilityClass;
import java.util.*;

/**
 * OpenMetadata Test Data Factory for creating mock metadata.
 * Generates:
 * - Service configurations (PostgreSQL, Neo4j)
 * - Database and schema metadata
 * - Table and column definitions
 * - Lineage edges between tables
 * - Asset ownership and classifications
 *
 * All Korean data is provided for names and descriptions.
 */
@UtilityClass
public class TestOpenMetadataFactory {

    // ============= Metadata Models =============

    /**
     * Represents OpenMetadata Service configuration
     */
    public static class ServiceConfig {
        public String name;
        public String displayName;
        public String serviceType;
        public String description;
        public Map<String, Object> connection;

        public ServiceConfig(String name, String displayName, String serviceType,
                            String description, Map<String, Object> connection) {
            this.name = name;
            this.displayName = displayName;
            this.serviceType = serviceType;
            this.description = description;
            this.connection = connection;
        }
    }

    /**
     * Represents OpenMetadata Database
     */
    public static class DatabaseMetadata {
        public String name;
        public String displayName;
        public String description;
        public String serviceId;

        public DatabaseMetadata(String name, String displayName, String description, String serviceId) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.serviceId = serviceId;
        }
    }

    /**
     * Represents OpenMetadata Schema
     */
    public static class SchemaMetadata {
        public String name;
        public String displayName;
        public String description;
        public String databaseId;

        public SchemaMetadata(String name, String displayName, String description, String databaseId) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.databaseId = databaseId;
        }
    }

    /**
     * Represents OpenMetadata Column
     */
    public static class ColumnMetadata {
        public String name;
        public String displayName;
        public String dataType;
        public String description;
        public Integer ordinalPosition;
        public Boolean nullable;

        public ColumnMetadata(String name, String displayName, String dataType,
                             String description, Integer ordinalPosition, Boolean nullable) {
            this.name = name;
            this.displayName = displayName;
            this.dataType = dataType;
            this.description = description;
            this.ordinalPosition = ordinalPosition;
            this.nullable = nullable;
        }
    }

    /**
     * Represents OpenMetadata Table
     */
    public static class TableMetadata {
        public String name;
        public String displayName;
        public String description;
        public String schemaId;
        public List<ColumnMetadata> columns;
        public String tableType;

        public TableMetadata(String name, String displayName, String description,
                            String schemaId, String tableType) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.schemaId = schemaId;
            this.tableType = tableType;
            this.columns = new ArrayList<>();
        }

        public void addColumn(ColumnMetadata column) {
            this.columns.add(column);
        }
    }

    /**
     * Represents OpenMetadata Lineage Edge
     */
    public static class LineageEdge {
        public String fromTableId;
        public String toTableId;
        public String edgeType;
        public String description;

        public LineageEdge(String fromTableId, String toTableId, String edgeType, String description) {
            this.fromTableId = fromTableId;
            this.toTableId = toTableId;
            this.edgeType = edgeType;
            this.description = description;
        }
    }

    /**
     * Represents OpenMetadata Asset with ownership and tags
     */
    public static class AssetMetadata {
        public String id;
        public String name;
        public String assetType;
        public String owner;
        public List<String> tags;
        public Map<String, String> customProperties;

        public AssetMetadata(String id, String name, String assetType, String owner) {
            this.id = id;
            this.name = name;
            this.assetType = assetType;
            this.owner = owner;
            this.tags = new ArrayList<>();
            this.customProperties = new HashMap<>();
        }

        public void addTag(String tag) {
            this.tags.add(tag);
        }

        public void addCustomProperty(String key, String value) {
            this.customProperties.put(key, value);
        }
    }

    // ============= Service Configuration Builders =============

    /**
     * Creates PostgreSQL service configuration
     */
    public static ServiceConfig createPostgreSQLService() {
        Map<String, Object> connection = new HashMap<>();
        connection.put("type", "Postgres");
        connection.put("hostPort", "localhost:5432");
        connection.put("database", "pms_db");
        connection.put("username", "pms_user");
        // Password should be handled securely in real scenarios

        return new ServiceConfig(
                "pms-postgres-service",
                "PMS PostgreSQL 서비스",
                "Postgres",
                "보험심사 관리 시스템의 주 데이터베이스",
                connection
        );
    }

    /**
     * Creates Neo4j service configuration
     */
    public static ServiceConfig createNeo4jService() {
        Map<String, Object> connection = new HashMap<>();
        connection.put("type", "Neo4j");
        connection.put("hostPort", "localhost:7687");
        connection.put("username", "neo4j");
        // Password should be handled securely

        return new ServiceConfig(
                "pms-neo4j-service",
                "PMS Neo4j 그래프 데이터베이스",
                "Neo4j",
                "요구사항과 작업 간의 lineage 관계를 저장하는 그래프 데이터베이스",
                connection
        );
    }

    // ============= Database & Schema Metadata Builders =============

    /**
     * Creates database metadata for PMS
     */
    public static DatabaseMetadata createPMSDatabase() {
        return new DatabaseMetadata(
                "pms_db",
                "PMS 데이터베이스",
                "보험심사 프로젝트 관리 시스템 메인 데이터베이스",
                "pms-postgres-service"
        );
    }

    /**
     * Creates schema metadata
     */
    public static List<SchemaMetadata> createSchemas(String databaseId) {
        List<SchemaMetadata> schemas = new ArrayList<>();
        schemas.add(new SchemaMetadata(
                "auth",
                "인증 및 권한 스키마",
                "사용자, 역할, 권한 관련 테이블",
                databaseId
        ));
        schemas.add(new SchemaMetadata(
                "project",
                "프로젝트 스키마",
                "프로젝트, 페이즈, 요구사항 관련 테이블",
                databaseId
        ));
        schemas.add(new SchemaMetadata(
                "task",
                "작업 관리 스키마",
                "Sprint, 사용자 스토리, 작업 관련 테이블",
                databaseId
        ));
        schemas.add(new SchemaMetadata(
                "chat",
                "채팅 스키마",
                "채팅 세션 및 메시지",
                databaseId
        ));
        return schemas;
    }

    // ============= Table Metadata Builders =============

    /**
     * Creates table metadata for auth schema
     */
    public static List<TableMetadata> createAuthSchemaTables(String schemaId) {
        List<TableMetadata> tables = new ArrayList<>();

        // Users table
        TableMetadata usersTable = new TableMetadata(
                "users",
                "사용자",
                "시스템 사용자 정보",
                schemaId,
                "Table"
        );
        usersTable.addColumn(new ColumnMetadata("id", "ID", "varchar", "사용자 ID", 1, false));
        usersTable.addColumn(new ColumnMetadata("email", "이메일", "varchar", "사용자 이메일", 2, false));
        usersTable.addColumn(new ColumnMetadata("name", "이름", "varchar", "사용자 이름", 3, false));
        usersTable.addColumn(new ColumnMetadata("role", "역할", "varchar", "시스템 역할", 4, false));
        usersTable.addColumn(new ColumnMetadata("active", "활성", "boolean", "활성 여부", 5, false));
        tables.add(usersTable);

        return tables;
    }

    /**
     * Creates table metadata for project schema
     */
    public static List<TableMetadata> createProjectSchemaTables(String schemaId) {
        List<TableMetadata> tables = new ArrayList<>();

        // Projects table
        TableMetadata projectsTable = new TableMetadata(
                "projects",
                "프로젝트",
                "프로젝트 마스터 정보",
                schemaId,
                "Table"
        );
        projectsTable.addColumn(new ColumnMetadata("id", "ID", "varchar", "프로젝트 ID", 1, false));
        projectsTable.addColumn(new ColumnMetadata("name", "이름", "varchar", "프로젝트 이름", 2, false));
        projectsTable.addColumn(new ColumnMetadata("status", "상태", "varchar", "프로젝트 상태", 3, false));
        projectsTable.addColumn(new ColumnMetadata("progress", "진행률", "integer", "진행 백분율", 4, true));
        tables.add(projectsTable);

        // Requirements table
        TableMetadata requirementsTable = new TableMetadata(
                "requirements",
                "요구사항",
                "프로젝트 요구사항",
                schemaId,
                "Table"
        );
        requirementsTable.addColumn(new ColumnMetadata("id", "ID", "varchar", "요구사항 ID", 1, false));
        requirementsTable.addColumn(new ColumnMetadata("code", "코드", "varchar", "요구사항 코드", 2, false));
        requirementsTable.addColumn(new ColumnMetadata("title", "제목", "varchar", "요구사항 제목", 3, false));
        requirementsTable.addColumn(new ColumnMetadata("category", "카테고리", "varchar", "요구사항 카테고리", 4, true));
        requirementsTable.addColumn(new ColumnMetadata("status", "상태", "varchar", "요구사항 상태", 5, false));
        requirementsTable.addColumn(new ColumnMetadata("project_id", "프로젝트ID", "varchar", "소속 프로젝트", 6, false));
        requirementsTable.addColumn(new ColumnMetadata("neo4j_node_id", "Neo4j 노드ID", "varchar", "Neo4j 그래프 노드 ID", 7, true));
        tables.add(requirementsTable);

        // RFPs table
        TableMetadata rfpsTable = new TableMetadata(
                "rfps",
                "제안요청서",
                "요구사항 정의 문서",
                schemaId,
                "Table"
        );
        rfpsTable.addColumn(new ColumnMetadata("id", "ID", "varchar", "RFP ID", 1, false));
        rfpsTable.addColumn(new ColumnMetadata("title", "제목", "varchar", "RFP 제목", 2, false));
        rfpsTable.addColumn(new ColumnMetadata("status", "상태", "varchar", "RFP 상태", 3, false));
        rfpsTable.addColumn(new ColumnMetadata("project_id", "프로젝트ID", "varchar", "소속 프로젝트", 4, false));
        tables.add(rfpsTable);

        return tables;
    }

    /**
     * Creates table metadata for task schema
     */
    public static List<TableMetadata> createTaskSchemaTables(String schemaId) {
        List<TableMetadata> tables = new ArrayList<>();

        // User stories table
        TableMetadata storiesTable = new TableMetadata(
                "user_stories",
                "사용자 스토리",
                "Sprint에 포함된 개발 단위",
                schemaId,
                "Table"
        );
        storiesTable.addColumn(new ColumnMetadata("id", "ID", "varchar", "스토리 ID", 1, false));
        storiesTable.addColumn(new ColumnMetadata("title", "제목", "varchar", "스토리 제목", 2, false));
        storiesTable.addColumn(new ColumnMetadata("status", "상태", "varchar", "스토리 상태", 3, false));
        storiesTable.addColumn(new ColumnMetadata("project_id", "프로젝트ID", "varchar", "소속 프로젝트", 4, false));
        storiesTable.addColumn(new ColumnMetadata("sprint_id", "SprintID", "varchar", "Sprint ID", 5, true));
        tables.add(storiesTable);

        // Tasks table
        TableMetadata tasksTable = new TableMetadata(
                "tasks",
                "작업",
                "Kanban 보드의 작업 항목",
                schemaId,
                "Table"
        );
        tasksTable.addColumn(new ColumnMetadata("id", "ID", "varchar", "작업 ID", 1, false));
        tasksTable.addColumn(new ColumnMetadata("title", "제목", "varchar", "작업 제목", 2, false));
        tasksTable.addColumn(new ColumnMetadata("status", "상태", "varchar", "작업 상태", 3, false));
        tasksTable.addColumn(new ColumnMetadata("priority", "우선순위", "varchar", "우선순위", 4, false));
        tasksTable.addColumn(new ColumnMetadata("column_id", "컬럼ID", "varchar", "Kanban 컬럼 ID", 5, false));
        tables.add(tasksTable);

        // Sprints table
        TableMetadata sprintsTable = new TableMetadata(
                "sprints",
                "스프린트",
                "2주 단위 개발 주기",
                schemaId,
                "Table"
        );
        sprintsTable.addColumn(new ColumnMetadata("id", "ID", "varchar", "Sprint ID", 1, false));
        sprintsTable.addColumn(new ColumnMetadata("name", "이름", "varchar", "Sprint 이름", 2, false));
        sprintsTable.addColumn(new ColumnMetadata("status", "상태", "varchar", "Sprint 상태", 3, false));
        sprintsTable.addColumn(new ColumnMetadata("project_id", "프로젝트ID", "varchar", "소속 프로젝트", 4, false));
        tables.add(sprintsTable);

        return tables;
    }

    // ============= Lineage Builders =============

    /**
     * Creates lineage edges representing data flow
     */
    public static List<LineageEdge> createLineageEdges() {
        List<LineageEdge> edges = new ArrayList<>();

        // Requirement -> UserStory lineage
        // (요구사항이 사용자 스토리로 분해됨)
        edges.add(new LineageEdge(
                "requirements",
                "user_stories",
                "DERIVES_FROM",
                "요구사항이 사용자 스토리로 분해"
        ));

        // UserStory -> Task lineage
        // (사용자 스토리가 작업으로 분해됨)
        edges.add(new LineageEdge(
                "user_stories",
                "tasks",
                "BREAKS_DOWN_TO",
                "사용자 스토리가 작업으로 분해"
        ));

        // Task -> Sprint lineage
        // (작업이 Sprint에 배정됨)
        edges.add(new LineageEdge(
                "tasks",
                "sprints",
                "BELONGS_TO",
                "작업이 Sprint에 배정"
        ));

        // RFP -> Requirements lineage
        // (제안요청서가 요구사항을 정의)
        edges.add(new LineageEdge(
                "rfps",
                "requirements",
                "DEFINES",
                "제안요청서가 요구사항을 정의"
        ));

        // Project -> Requirements lineage
        edges.add(new LineageEdge(
                "projects",
                "requirements",
                "OWNS",
                "프로젝트가 요구사항을 소유"
        ));

        // Project -> UserStories lineage
        edges.add(new LineageEdge(
                "projects",
                "user_stories",
                "OWNS",
                "프로젝트가 사용자 스토리를 소유"
        ));

        return edges;
    }

    // ============= Asset Ownership & Classification =============

    /**
     * Creates asset metadata with ownership information
     */
    public static List<AssetMetadata> createAssets(String projectId) {
        List<AssetMetadata> assets = new ArrayList<>();

        // Requirements asset
        AssetMetadata requirementsAsset = new AssetMetadata(
                "requirements-" + projectId,
                "프로젝트 요구사항",
                "Table",
                "user-003"  // PM owner
        );
        requirementsAsset.addTag("requirements");
        requirementsAsset.addTag("critical");
        requirementsAsset.addCustomProperty("projectId", projectId);
        requirementsAsset.addCustomProperty("businessOwner", "user-001");
        assets.add(requirementsAsset);

        // UserStories asset
        AssetMetadata storiesAsset = new AssetMetadata(
                "user_stories-" + projectId,
                "개발 사용자 스토리",
                "Table",
                "user-003"  // PM owner
        );
        storiesAsset.addTag("development");
        storiesAsset.addTag("agile");
        storiesAsset.addCustomProperty("projectId", projectId);
        assets.add(storiesAsset);

        // Tasks asset
        AssetMetadata tasksAsset = new AssetMetadata(
                "tasks-" + projectId,
                "작업 항목",
                "Table",
                "user-002"  // PMO_HEAD owner
        );
        tasksAsset.addTag("development");
        tasksAsset.addTag("tracking");
        tasksAsset.addCustomProperty("projectId", projectId);
        assets.add(tasksAsset);

        // Sprints asset
        AssetMetadata sprintsAsset = new AssetMetadata(
                "sprints-" + projectId,
                "Sprint 계획",
                "Table",
                "user-003"  // PM owner
        );
        sprintsAsset.addTag("planning");
        sprintsAsset.addTag("agile");
        sprintsAsset.addCustomProperty("projectId", projectId);
        assets.add(sprintsAsset);

        return assets;
    }

    // ============= Complete Metadata Structure =============

    /**
     * Represents complete OpenMetadata structure
     */
    public static class MetadataStructure {
        public ServiceConfig postgresService;
        public ServiceConfig neo4jService;
        public DatabaseMetadata database;
        public List<SchemaMetadata> schemas;
        public List<TableMetadata> tables;
        public List<LineageEdge> lineageEdges;
        public List<AssetMetadata> assets;

        public MetadataStructure(String projectId) {
            this.postgresService = createPostgreSQLService();
            this.neo4jService = createNeo4jService();
            this.database = createPMSDatabase();
            this.schemas = createSchemas(database.name);

            this.tables = new ArrayList<>();
            for (SchemaMetadata schema : schemas) {
                if (schema.name.equals("auth")) {
                    tables.addAll(createAuthSchemaTables(schema.name));
                } else if (schema.name.equals("project")) {
                    tables.addAll(createProjectSchemaTables(schema.name));
                } else if (schema.name.equals("task")) {
                    tables.addAll(createTaskSchemaTables(schema.name));
                }
            }

            this.lineageEdges = createLineageEdges();
            this.assets = createAssets(projectId);
        }

        public int getTotalTableCount() {
            return tables.size();
        }

        public int getTotalSchemaCount() {
            return schemas.size();
        }

        public int getTotalLineageEdgeCount() {
            return lineageEdges.size();
        }

        public int getTotalAssetCount() {
            return assets.size();
        }
    }

    /**
     * Creates complete metadata structure for testing
     */
    public static MetadataStructure createCompleteMetadataStructure(String projectId) {
        return new MetadataStructure(projectId);
    }

    // ============= REST API JSON Generators =============

    /**
     * Generates OpenMetadata API request JSON for service creation
     */
    public static String generateServiceCreateJson(ServiceConfig service) {
        return String.format(
                "{\n" +
                "  \"name\": \"%s\",\n" +
                "  \"displayName\": \"%s\",\n" +
                "  \"serviceType\": \"%s\",\n" +
                "  \"description\": \"%s\"\n" +
                "}",
                service.name, service.displayName, service.serviceType, service.description
        );
    }

    /**
     * Generates OpenMetadata API request JSON for table creation
     */
    public static String generateTableCreateJson(TableMetadata table) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(table.name).append("\",\n");
        sb.append("  \"displayName\": \"").append(table.displayName).append("\",\n");
        sb.append("  \"description\": \"").append(table.description).append("\",\n");
        sb.append("  \"tableType\": \"").append(table.tableType).append("\",\n");
        sb.append("  \"columns\": [\n");

        for (int i = 0; i < table.columns.size(); i++) {
            ColumnMetadata col = table.columns.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(col.name).append("\",\n");
            sb.append("      \"displayName\": \"").append(col.displayName).append("\",\n");
            sb.append("      \"dataType\": \"").append(col.dataType).append("\",\n");
            sb.append("      \"description\": \"").append(col.description).append("\",\n");
            sb.append("      \"ordinalPosition\": ").append(col.ordinalPosition).append("\n");
            sb.append("    }");
            if (i < table.columns.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates OpenMetadata API request JSON for lineage creation
     */
    public static String generateLineageCreateJson(LineageEdge edge) {
        return String.format(
                "{\n" +
                "  \"entity\": { \"id\": \"%s\" },\n" +
                "  \"lineageDetails\": {\n" +
                "    \"edgeType\": \"%s\",\n" +
                "    \"description\": \"%s\",\n" +
                "    \"source\": { \"id\": \"%s\" },\n" +
                "    \"target\": { \"id\": \"%s\" }\n" +
                "  }\n" +
                "}",
                edge.fromTableId, edge.edgeType, edge.description,
                edge.fromTableId, edge.toTableId
        );
    }
}
