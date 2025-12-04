# GitHub 템플릿 설정 가이드

이 문서는 Loopers Spring Java 템플릿 프로젝트의 GitHub 템플릿(PR 템플릿, 커밋 메시지 템플릿)을 설정하는 방법을 안내합니다.

## 📋 목차

- [커밋 메시지 템플릿 설정](#커밋-메시지-템플릿-설정)
- [PR 템플릿 사용법](#pr-템플릿-사용법)
- [GitHub Copilot 지침 활용](#github-copilot-지침-활용)
- [템플릿 검증 체크리스트](#템플릿-검증-체크리스트)

## 커밋 메시지 템플릿 설정

### 로컬 저장소에만 적용

프로젝트 루트 디렉토리에서 다음 명령어를 실행합니다:

```bash
git config commit.template .github/commit_template.txt
```

이제 `git commit` 명령어를 실행하면 템플릿이 자동으로 로드됩니다.

### 전역 설정 (모든 저장소에 적용)

모든 Git 저장소에서 이 템플릿을 사용하려면:

```bash
# 1. 템플릿 파일을 홈 디렉토리로 복사
cp .github/commit_template.txt ~/.github/commit_template.txt

# 2. 전역 설정 적용
git config --global commit.template ~/.github/commit_template.txt
```

### 설정 확인

```bash
# 로컬 설정 확인
git config commit.template

# 전역 설정 확인
git config --global commit.template
```

### 커밋 메시지 작성 예시

#### 1. 새로운 기능 추가

```
feat(domain): UserEntity에 포인트 충전 로직 추가

- BaseEntity를 상속하여 생명주기 관리
- chargePoint 메서드로 포인트 충전 규칙 캡슐화
- 충전 금액 검증 로직 포함 (0보다 큰 값, 최대 100만원)
- guard() 메서드 오버라이드하여 포인트 음수 방지

Closes #123
```

#### 2. 버그 수정

```
fix(infrastructure): UserRepositoryImpl 페이징 쿼리 성능 개선

- 카운트 쿼리와 데이터 조회 쿼리 분리
- QueryDSL을 활용한 동적 쿼리 최적화
- 불필요한 조인 제거

Refs #456
```

#### 3. 테스트 코드 추가

```
test(domain): UserService 단위 테스트 추가

- 한국어 테스트 메서드명 사용
- @DisplayName으로 테스트 의도 명확화
- Given-When-Then 구조 적용
- @Nested로 테스트 그룹화
```

#### 4. 리팩토링

```
refactor(application): UserFacade 트랜잭션 경계 개선

- 여러 도메인 서비스 호출을 하나의 트랜잭션으로 묶음
- DTO 변환 로직을 Facade에서 처리
- 읽기 전용 트랜잭션 적용
```

### 커밋 타입 가이드

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | `feat(domain): 포인트 충전 기능 추가` |
| `fix` | 버그 수정 | `fix(infrastructure): 페이징 쿼리 오류 수정` |
| `refactor` | 코드 리팩토링 (기능 변경 없음) | `refactor(application): Facade 구조 개선` |
| `test` | 테스트 코드 추가/수정 | `test(domain): UserService 테스트 추가` |
| `docs` | 문서 수정 | `docs: README 업데이트` |
| `style` | 코드 포맷팅 (기능 변경 없음) | `style(domain): 코드 포맷팅 적용` |
| `chore` | 빌드 설정, 패키지 매니저 설정 | `chore: Gradle 의존성 업데이트` |

### Scope 가이드 (DDD 레이어 기반)

| Scope | 설명 | 예시 |
|-------|------|------|
| `domain` | Domain Layer (Entity, Service, Repository Interface) | `feat(domain): UserEntity 추가` |
| `application` | Application Layer (Facade, DTO, Command) | `feat(application): UserFacade 추가` |
| `infrastructure` | Infrastructure Layer (Repository Impl, JPA) | `feat(infrastructure): UserRepositoryImpl 추가` |
| `interface` | Interface Layer (Controller, API Spec) | `feat(interface): UserV1Controller 추가` |
| `config` | 설정 파일 (application.yml, Config 클래스) | `chore(config): 데이터베이스 설정 추가` |
| `test` | 테스트 코드 | `test(test): E2E 테스트 추가` |

## PR 템플릿 사용법

### PR 생성 시 자동 적용

GitHub에서 Pull Request를 생성하면 `.github/pull_request_template.md` 파일의 내용이 자동으로 PR 설명에 로드됩니다.

### PR 템플릿 섹션 가이드

#### 1. Summary (필수)

변경 사항의 배경과 목적을 간단히 설명합니다.

```markdown
## 📌 Summary

사용자 포인트 충전 기능을 구현했습니다.

- UserEntity에 포인트 충전 비즈니스 로직 추가
- 충전 금액 검증 및 이력 기록 기능 포함
- 한국 표준시(Asia/Seoul) 기준으로 시간 처리
```

#### 2. Review Points (필수)

리뷰어가 중점적으로 확인해야 할 부분을 명시합니다.

```markdown
## 💬 Review Points

1. **포인트 충전 비즈니스 로직**: UserEntity의 chargePoint 메서드가 비즈니스 규칙을 올바르게 구현했는지 확인 부탁드립니다.
2. **예외 처리**: 충전 금액이 0 이하이거나 최대 한도를 초과하는 경우 적절한 예외가 발생하는지 확인해주세요.
3. **테스트 커버리지**: 경계값 테스트가 충분한지 검토 부탁드립니다.
```

#### 3. Architecture & Design (필수)

DDD 레이어드 아키텍처 준수 여부를 체크합니다.

```markdown
## 🏗️ Architecture & Design

### 레이어 분리 및 의존성
- [x] Domain, Application, Infrastructure, Interface 레이어가 올바르게 분리되어 있다
- [x] 의존성 방향이 올바르다 (Interface → Application → Domain ← Infrastructure)
- [x] 순환 의존성이 없다

### Repository 패턴
- [x] Repository 인터페이스가 Domain Layer에 정의되어 있다
- [x] Repository 구현체가 Infrastructure Layer에 위치한다
- [x] JPA Repository를 적절히 활용하고 있다
```

#### 4. Code Quality Checklist (필수)

코드 품질 기준을 체크합니다.

```markdown
## ✅ Code Quality Checklist

### 네이밍 컨벤션
- [x] 클래스명: PascalCase 사용
- [x] 메서드명: camelCase 사용, 의미 있는 동사로 시작
- [x] 변수명: camelCase 사용, 의미가 명확함
- [x] 상수명: UPPER_SNAKE_CASE 사용

### 엔티티 설계
- [x] BaseEntity를 상속하여 생명주기 관리
- [x] 정적 팩토리 메서드를 통한 객체 생성
- [x] 비즈니스 로직이 도메인 객체 내부에 캡슐화되어 있다
```

#### 5. Testing Checklist (필수)

테스트 코드 품질을 체크합니다.

```markdown
## 🧪 Testing Checklist

### 테스트 구조
- [x] 한국어 테스트 메서드명 사용 (언더스코어 구분)
- [x] @DisplayName으로 테스트 의도 명확히 표현
- [x] Given-When-Then 구조 준수
- [x] @Nested를 활용한 테스트 그룹화

### 테스트 커버리지
- [x] 새로운 기능에 대한 테스트 코드 포함
- [x] 예외 상황에 대한 테스트 포함
- [x] 경계값 테스트 포함 (필요시)
```

#### 6. References (선택사항)

관련 이슈나 문서 링크를 추가합니다.

```markdown
## 📎 References

### 관련 이슈/문서
- Closes #123
- 관련 설계 문서: [링크]

### Steering 문서 참조
- [아키텍처 패턴](.kiro/steering/architecture-patterns.md)
- [자바 코딩 컨벤션](.kiro/steering/java-conventions.md)
```

## GitHub Copilot 지침 활용

### Copilot 지침 파일 위치

`.github/copilot-instructions.md` 파일에 프로젝트의 코딩 표준과 패턴이 정의되어 있습니다.

### Copilot이 자동으로 참조하는 내용

GitHub Copilot은 다음 내용을 자동으로 참조하여 코드를 생성합니다:

1. **아키텍처 패턴**: DDD 레이어드 아키텍처 구조
2. **코딩 컨벤션**: 네이밍 규칙, 포맷팅 규칙
3. **엔티티 설계**: BaseEntity 상속, 정적 팩토리 메서드
4. **Repository 패턴**: 3계층 Repository 구조
5. **Service 패턴**: Domain Service vs Application Facade
6. **예외 처리**: CoreException 기반 예외 체계
7. **테스팅 표준**: 한국어 테스트 메서드명, Given-When-Then
8. **시간 처리**: ZonedDateTime, Asia/Seoul
9. **한국어 개발 환경**: 한국어 에러 메시지, 로그, 주석

### Copilot 활용 팁

1. **주석으로 의도 표현**: 한국어로 주석을 작성하면 Copilot이 프로젝트 표준에 맞는 코드를 제안합니다.

```java
// 사용자 포인트 충전 메서드 - BaseEntity 상속, 비즈니스 로직 캡슐화
public void chargePoint(BigDecimal amount) {
    // Copilot이 자동으로 검증 로직과 충전 로직을 제안합니다
}
```

2. **테스트 코드 생성**: 한국어 테스트 메서드명을 작성하면 Given-When-Then 구조의 테스트를 제안합니다.

```java
@Test
@DisplayName("유효한 금액으로 포인트를 충전하면 성공한다")
void 유효한_금액으로_포인트를_충전하면_성공한다() {
    // Copilot이 자동으로 Given-When-Then 구조의 테스트를 제안합니다
}
```

## 템플릿 검증 체크리스트

### 커밋 메시지 템플릿 검증

- [ ] 커밋 메시지 템플릿이 로컬 또는 전역으로 설정되어 있다
- [ ] `git commit` 실행 시 템플릿이 자동으로 로드된다
- [ ] Conventional Commits 형식을 따른다
- [ ] DDD 레이어 기반 scope가 정의되어 있다
- [ ] 한국어 가이드 및 예시가 포함되어 있다

### PR 템플릿 검증

- [ ] PR 생성 시 템플릿이 자동으로 로드된다
- [ ] Architecture & Design 섹션이 포함되어 있다
- [ ] Code Quality Checklist가 포함되어 있다
- [ ] Testing Checklist가 포함되어 있다
- [ ] Steering 문서 참조 링크가 포함되어 있다

### Copilot 지침 검증

- [ ] `.github/copilot-instructions.md` 파일이 존재한다
- [ ] 프로젝트 개요가 작성되어 있다
- [ ] 아키텍처 패턴이 설명되어 있다
- [ ] 코딩 컨벤션이 정의되어 있다
- [ ] 엔티티 설계 가이드가 포함되어 있다
- [ ] Repository 패턴이 설명되어 있다
- [ ] Service 패턴이 설명되어 있다
- [ ] 예외 처리 표준이 정의되어 있다
- [ ] 테스팅 표준이 포함되어 있다
- [ ] 시간 처리 가이드가 작성되어 있다
- [ ] 한국어 개발 환경 가이드가 포함되어 있다
- [ ] Steering 문서 참조 링크가 포함되어 있다

## 참고 문서

프로젝트의 상세한 표준과 가이드는 `.kiro/steering` 디렉토리의 문서를 참조하세요:

- **아키텍처 패턴**: [.kiro/steering/architecture-patterns.md](../.kiro/steering/architecture-patterns.md)
- **자바 코딩 컨벤션**: [.kiro/steering/java-conventions.md](../.kiro/steering/java-conventions.md)
- **데이터베이스 패턴**: [.kiro/steering/database-patterns.md](../.kiro/steering/database-patterns.md)
- **예외 처리 표준**: [.kiro/steering/error-handling.md](../.kiro/steering/error-handling.md)
- **테스팅 표준**: [.kiro/steering/testing-standards.md](../.kiro/steering/testing-standards.md)
- **한국어 개발 환경**: [.kiro/steering/korean-development.md](../.kiro/steering/korean-development.md)
- **인프라 구성**: [.kiro/steering/infrastructure-setup.md](../.kiro/steering/infrastructure-setup.md)
- **코드 리뷰 체크리스트**: [.kiro/steering/code-review-checklist.md](../.kiro/steering/code-review-checklist.md)

## 문제 해결

### 커밋 템플릿이 로드되지 않는 경우

```bash
# 설정 확인
git config commit.template

# 설정이 없다면 다시 설정
git config commit.template .github/commit_template.txt

# 파일 경로가 올바른지 확인
ls -la .github/commit_template.txt
```

### PR 템플릿이 자동으로 로드되지 않는 경우

1. 파일 이름이 정확한지 확인: `.github/pull_request_template.md`
2. 파일이 기본 브랜치(main 또는 master)에 커밋되어 있는지 확인
3. GitHub 웹 인터페이스에서 PR을 생성하는지 확인 (CLI 도구는 템플릿을 자동으로 로드하지 않을 수 있음)

### Copilot이 프로젝트 표준을 따르지 않는 경우

1. `.github/copilot-instructions.md` 파일이 존재하는지 확인
2. 파일이 기본 브랜치에 커밋되어 있는지 확인
3. Copilot 설정에서 "Use instructions from .github/copilot-instructions.md" 옵션이 활성화되어 있는지 확인
4. 주석으로 명확한 의도를 표현하여 Copilot이 더 정확한 제안을 할 수 있도록 유도

## 추가 지원

템플릿 사용 중 문제가 발생하거나 개선 사항이 있다면 이슈를 생성해주세요.
