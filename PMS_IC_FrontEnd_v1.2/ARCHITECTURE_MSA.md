# PMS 백엔드 아키텍처: Monolith & MSA 지원

## 🎯 전략: Modular Monolith First → MSA Ready

### Phase 1: Modular Monolith (초기 구현)
- 단일 Spring Boot 애플리케이션
- 내부 모듈화 (패키지 분리)
- MSA 전환 용이한 구조

### Phase 2: MSA 전환 (필요시)
- 각 모듈을 독립 서비스로 분리
- Service Mesh 도입
- API Gateway 추가

---

## 🏛️ MSA 서비스 분해 (Bounded Context)

```
┌─────────────────────────────────────────────────────────┐
│                   API Gateway (Kong/Spring Cloud Gateway) │
│                   - 라우팅, 인증, Rate Limiting            │
└─────────────────────┬───────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┬─────────────┐
        │             │             │             │
        ↓             ↓             ↓             ↓
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Auth Service │ │ Project Mgmt │ │ Task Service │ │ Chat Service │
│              │ │   Service    │ │              │ │              │
│ - 로그인      │ │ - 프로젝트    │ │ - 칸반보드    │ │ - 챗봇       │
│ - JWT        │ │ - 단계관리    │ │ - 백로그      │ │ - AI 연동    │
│ - RBAC       │ │ - Gate 승인   │ │ - 스프린트    │ │ - 히스토리   │
│              │ │ - 산출물      │ │              │ │              │
│ PostgreSQL   │ │ PostgreSQL   │ │ PostgreSQL   │ │ Redis        │
│ Redis        │ │              │ │              │ │ PostgreSQL   │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                      │
              ┌───────┴────────┐
              │  Service Mesh  │
              │  (Istio/Envoy) │
              └────────────────┘
```

### 추가 서비스 (확장 시)

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Risk Service │ │Report Service│ │ Notif Service│
│              │ │              │ │              │
│ - 리스크     │ │ - 대시보드    │ │ - 알림       │
│ - 이슈       │ │ - 리포팅      │ │ - 이메일     │
│              │ │              │ │              │
│ PostgreSQL   │ │ PostgreSQL   │ │ Redis        │
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## 📦 Modular Monolith 구조 (Phase 1)

```
pms-backend/
├── pom.xml                          # Parent POM
├── src/main/java/com/insuretech/pms/
│   ├── PmsApplication.java          # Main Application
│   │
│   ├── common/                      # 공통 모듈
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   └── JpaConfig.java
│   │   ├── exception/
│   │   └── util/
│   │
│   ├── auth/                        # 인증/인가 모듈 ⭐ MSA 분리 대상
│   │   ├── domain/
│   │   │   ├── User.java
│   │   │   └── Role.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   └── JwtTokenProvider.java
│   │   └── controller/
│   │       └── AuthController.java
│   │
│   ├── project/                     # 프로젝트 관리 모듈 ⭐ MSA 분리 대상
│   │   ├── domain/
│   │   │   ├── Project.java
│   │   │   ├── Phase.java
│   │   │   ├── PhaseGate.java
│   │   │   └── Deliverable.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── ProjectService.java
│   │   │   ├── PhaseService.java
│   │   │   └── GateApprovalService.java
│   │   └── controller/
│   │       ├── ProjectController.java
│   │       └── PhaseController.java
│   │
│   ├── task/                        # 태스크 관리 모듈 ⭐ MSA 분리 대상
│   │   ├── domain/
│   │   │   ├── KanbanColumn.java
│   │   │   ├── Task.java
│   │   │   ├── UserStory.java
│   │   │   └── Sprint.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── KanbanService.java
│   │   │   ├── BacklogService.java
│   │   │   └── SprintService.java
│   │   └── controller/
│   │       ├── TaskController.java
│   │       └── SprintController.java
│   │
│   ├── chat/                        # 챗봇 모듈 ⭐ MSA 분리 대상
│   │   ├── domain/
│   │   │   ├── ChatSession.java
│   │   │   └── ChatMessage.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── ChatService.java
│   │   │   ├── ChatHistoryService.java
│   │   │   └── AIChatClient.java
│   │   └── controller/
│   │       └── ChatController.java
│   │
│   ├── risk/                        # 리스크/이슈 모듈 ⭐ MSA 분리 대상
│   │   ├── domain/
│   │   │   ├── Risk.java
│   │   │   └── Issue.java
│   │   ├── repository/
│   │   ├── service/
│   │   └── controller/
│   │
│   └── report/                      # 리포팅 모듈 ⭐ MSA 분리 대상
│       ├── domain/
│       ├── service/
│       │   ├── DashboardService.java
│       │   └── ReportExportService.java
│       └── controller/
│           └── ReportController.java
│
└── src/main/resources/
    ├── application.yml              # 기본 설정
    ├── application-dev.yml          # 개발 환경
    ├── application-prod.yml         # 프로덕션 환경
    └── application-msa.yml          # MSA 환경
```

---

## 🔧 MSA Ready 설계 원칙

### 1. **모듈 간 통신 인터페이스화**

```java
// ❌ 나쁜 예: 직접 의존
@Service
public class ProjectService {
    @Autowired
    private UserRepository userRepository;  // auth 모듈 직접 참조
}

// ✅ 좋은 예: 인터페이스 사용
@Service
public class ProjectService {
    private final UserClient userClient;  // 인터페이스

    // Monolith: 내부 호출
    // MSA: HTTP/gRPC 호출
}
```

### 2. **각 모듈의 독립 데이터베이스 스키마**

```sql
-- auth 스키마
CREATE SCHEMA auth;
CREATE TABLE auth.users (...);
CREATE TABLE auth.roles (...);

-- project 스키마
CREATE SCHEMA project;
CREATE TABLE project.projects (...);
CREATE TABLE project.phases (...);

-- task 스키마
CREATE SCHEMA task;
CREATE TABLE task.kanban_columns (...);
CREATE TABLE task.tasks (...);

-- chat 스키마
CREATE SCHEMA chat;
CREATE TABLE chat.sessions (...);
CREATE TABLE chat.messages (...);
```

### 3. **이벤트 기반 통신 준비**

```java
// 이벤트 발행
@Service
public class PhaseService {
    private final ApplicationEventPublisher eventPublisher;

    public void approveGate(String phaseId) {
        // 승인 로직

        // 이벤트 발행 (Monolith: 내부, MSA: Kafka)
        eventPublisher.publishEvent(
            new PhaseGateApprovedEvent(phaseId)
        );
    }
}

// 이벤트 수신
@Component
public class NotificationEventHandler {
    @EventListener
    public void handlePhaseGateApproved(PhaseGateApprovedEvent event) {
        // 알림 전송
    }
}
```

---

## 🌐 MSA 전환 시 추가 구성요소

### 1. **API Gateway (Spring Cloud Gateway)**

```yaml
# gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**

        - id: project-service
          uri: lb://project-service
          predicates:
            - Path=/api/projects/**

        - id: task-service
          uri: lb://task-service
          predicates:
            - Path=/api/tasks/**, /api/sprints/**

        - id: chat-service
          uri: lb://chat-service
          predicates:
            - Path=/api/chat/**
```

### 2. **Service Discovery (Eureka)**

```java
// 각 서비스에 추가
@SpringBootApplication
@EnableEurekaClient
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

### 3. **분산 트레이싱 (Zipkin/Jaeger)**

```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0
  zipkin:
    base-url: http://zipkin-server:9411
```

### 4. **Config Server (중앙 설정 관리)**

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/company/pms-config
          default-label: main
```

### 5. **Message Broker (Kafka/RabbitMQ)**

```java
// 이벤트 발행 (Kafka)
@Service
public class PhaseService {
    private final KafkaTemplate<String, PhaseGateApprovedEvent> kafkaTemplate;

    public void approveGate(String phaseId) {
        // 승인 로직

        kafkaTemplate.send("phase-events",
            new PhaseGateApprovedEvent(phaseId)
        );
    }
}

// 이벤트 구독
@Service
public class NotificationService {
    @KafkaListener(topics = "phase-events")
    public void handlePhaseEvent(PhaseGateApprovedEvent event) {
        // 알림 전송
    }
}
```

---

## 📊 데이터베이스 전략 (MSA)

### 옵션 1: Database per Service (권장)

```
Auth Service     → PostgreSQL (auth_db)
Project Service  → PostgreSQL (project_db)
Task Service     → PostgreSQL (task_db)
Chat Service     → PostgreSQL (chat_db) + Redis
Report Service   → PostgreSQL (report_db) - Read Replica
```

**장점:**
- 완전한 서비스 독립성
- 스케일 아웃 자유로움

**단점:**
- 분산 트랜잭션 복잡
- 데이터 일관성 관리 어려움

### 옵션 2: Shared Database (초기)

```
모든 서비스 → 단일 PostgreSQL (스키마 분리)
```

**장점:**
- 트랜잭션 간단
- 조인 가능

**단점:**
- 서비스 간 결합도 높음

---

## 🔄 Monolith → MSA 전환 로드맵

### Phase 1: Modular Monolith (0-6개월)
```
✅ 단일 Spring Boot
✅ 모듈별 패키지 분리
✅ 인터페이스 기반 통신
✅ 스키마 분리
✅ 이벤트 기반 설계
```

### Phase 2: Strangler Pattern (6-12개월)
```
⏳ Chat Service 먼저 분리 (독립성 높음)
⏳ API Gateway 도입
⏳ Service Discovery
⏳ 나머지 서비스 점진적 분리
```

### Phase 3: Full MSA (12개월+)
```
🔜 Kafka 도입
🔜 Service Mesh (Istio)
🔜 분산 트레이싱
🔜 Circuit Breaker
```

---

## 🎯 추천 기술 스택

### Modular Monolith (Phase 1)
```yaml
Backend:
  Framework: Spring Boot 3.2
  Language: Java 17
  Database:
    - PostgreSQL 15 (스키마 분리)
    - Redis 7 (캐시, 세션)
  Build: Maven/Gradle

Architecture:
  Pattern: Modular Monolith
  Module Communication: Interface + Events
  Database: Single DB, Multiple Schemas
```

### MSA (Phase 2+)
```yaml
Additional Components:
  API Gateway: Spring Cloud Gateway
  Service Discovery: Eureka / Consul
  Config Server: Spring Cloud Config
  Message Broker: Kafka / RabbitMQ
  Service Mesh: Istio (선택)
  Monitoring: Prometheus + Grafana
  Tracing: Zipkin / Jaeger
  Log Aggregation: ELK Stack
```

---

## 🔐 보안 (MSA 환경)

### 1. **API Gateway에서 JWT 검증**
```java
// Gateway에서 한 번만 검증
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory {
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractToken(exchange);

            if (jwtTokenProvider.validateToken(token)) {
                // 서비스 간 통신용 헤더 추가
                exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();
            }

            return chain.filter(exchange);
        };
    }
}
```

### 2. **서비스 간 mTLS (Mutual TLS)**
```yaml
# Istio를 사용하면 자동
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
spec:
  mtls:
    mode: STRICT
```

---

## 📝 마이그레이션 체크리스트

### Monolith → MSA 전환 시 확인사항

- [ ] 각 모듈이 독립적으로 배포 가능한가?
- [ ] 모듈 간 직접 DB 참조가 없는가?
- [ ] 트랜잭션 경계가 명확한가?
- [ ] 이벤트 기반 통신이 구현되었는가?
- [ ] 각 서비스의 SLA가 정의되었는가?
- [ ] 모니터링/로깅 체계가 갖춰졌는가?
- [ ] Circuit Breaker 패턴이 적용되었는가?
- [ ] 분산 트랜잭션 전략이 있는가? (Saga Pattern)

---

## ✅ 최종 권장사항

### 초기 구현 (지금)
```
✅ Modular Monolith으로 시작
✅ MSA 전환 가능한 구조 설계
✅ 모듈별 패키지 분리
✅ 인터페이스 기반 통신
✅ 독립 스키마 설계
```

### MSA 전환 시점 판단 기준
- 사용자 1000명 초과
- 팀 크기 20명 초과
- 배포 빈도 주 1회 이상
- 모듈별 독립 스케일링 필요
- 다른 팀과 명확한 서비스 경계

**이 전략으로 지금은 빠르게 개발하고, 나중에 MSA로 전환 가능합니다!**
